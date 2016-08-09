/**
  ******************************************************************************
  * @file    stm32f4xx_usart.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the Universal synchronous asynchronous receiver
  *          transmitter (USART):           
  *           - Initialization and Configuration
  *           - Data transfers
  *           - Multi-Processor Communication
  *           - LIN mode
  *           - Half-duplex mode
  *           - Smartcard mode
  *           - IrDA mode
  *           - DMA transfers management
  *           - Interrupts and flags management 
  *           
  *  @verbatim
  *      
  *          ===================================================================
  *                                 How to use this driver
  *          ===================================================================
  *          1. Enable peripheral clock using the follwoing functions
  *             RCC_APB2PeriphClockCmd(RCC_APB2Periph_USARTx, ENABLE) for USART1 and USART6 
  *             RCC_APB1PeriphClockCmd(RCC_APB1Periph_USARTx, ENABLE) for USART2, USART3, UART4 or UART5.
  *
  *          2.  According to the USART mode, enable the GPIO clocks using 
  *              RCC_AHB1PeriphClockCmd() function. (The I/O can be TX, RX, CTS, 
  *              or/and SCLK). 
  *
  *          3. Peripheral's alternate function: 
  *                 - Connect the pin to the desired peripherals' Alternate 
  *                   Function (AF) using GPIO_PinAFConfig() function
  *                 - Configure the desired pin in alternate function by:
  *                   GPIO_InitStruct->GPIO_Mode = GPIO_Mode_AF
  *                 - Select the type, pull-up/pull-down and output speed via 
  *                   GPIO_PuPd, GPIO_OType and GPIO_Speed members
  *                 - Call GPIO_Init() function
  *        
  *          4. Program the Baud Rate, Word Length , Stop Bit, Parity, Hardware 
  *             flow control and Mode(Receiver/Transmitter) using the USART_Init()
  *             function.
  *
  *          5. For synchronous mode, enable the clock and program the polarity,
  *             phase and last bit using the USART_ClockInit() function.
  *
  *          5. Enable the NVIC and the corresponding interrupt using the function 
  *             USART_ITConfig() if you need to use interrupt mode. 
  *
  *          6. When using the DMA mode 
  *                   - Configure the DMA using DMA_Init() function
  *                   - Active the needed channel Request using USART_DMACmd() function
  * 
  *          7. Enable the USART using the USART_Cmd() function.
  * 
  *          8. Enable the DMA using the DMA_Cmd() function, when using DMA mode. 
  *
  *          Refer to Multi-Processor, LIN, half-duplex, Smartcard, IrDA sub-sections
  *          for more details
  *          
  *          In order to reach higher communication baudrates, it is possible to
  *          enable the oversampling by 8 mode using the function USART_OverSampling8Cmd().
  *          This function should be called after enabling the USART clock (RCC_APBxPeriphClockCmd())
  *          and before calling the function USART_Init().
  *          
  *  @endverbatim
  *        
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************  
  */ 

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_conf.h"
#include "stm32f4xx_usart.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup USART 
  * @brief USART driver modules
  * @{
  */

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/

/*!< USART CR1 register clear Mask ((~(uint16_t)0xE9F3)) */
#define CR1_CLEAR_MASK            ((uint16_t)(USART_CR1_M | USART_CR1_PCE | \
                                              USART_CR1_PS | USART_CR1_TE | \
                                              USART_CR1_RE))

/*!< USART CR2 register clock bits clear Mask ((~(uint16_t)0xF0FF)) */
#define CR2_CLOCK_CLEAR_MASK      ((uint16_t)(USART_CR2_CLKEN | USART_CR2_CPOL | \
                                              USART_CR2_CPHA | USART_CR2_LBCL))

/*!< USART CR3 register clear Mask ((~(uint16_t)0xFCFF)) */
#define CR3_CLEAR_MASK            ((uint16_t)(USART_CR3_RTSE | USART_CR3_CTSE))

/*!< USART Interrupts mask */
#define IT_MASK                   ((uint16_t)0x001F)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup USART_Private_Functions
  * @{
  */

/** @defgroup USART_Group1 Initialization and Configuration functions
 *  @brief   Initialization and Configuration functions 
 *
@verbatim   
 ===============================================================================
                  Initialization and Configuration functions
 ===============================================================================  

  This subsection provides a set of functions allowing to initialize the USART 
  in asynchronous and in synchronous modes.
   - For the asynchronous mode only these parameters can be configured: 
        - Baud Rate
        - Word Length 
        - Stop Bit
        - Parity: If the parity is enabled, then the MSB bit of the data written
          in the data register is transmitted but is changed by the parity bit.
          Depending on the frame length defined by the M bit (8-bits or 9-bits),
          the possible USART frame formats are as listed in the following table:
   +-------------------------------------------------------------+     
   |   M bit |  PCE bit  |            USART frame                |
   |---------------------|---------------------------------------|             
   |    0    |    0      |    | SB | 8 bit data | STB |          |
   |---------|-----------|---------------------------------------|  
   |    0    |    1      |    | SB | 7 bit data | PB | STB |     |
   |---------|-----------|---------------------------------------|  
   |    1    |    0      |    | SB | 9 bit data | STB |          |
   |---------|-----------|---------------------------------------|  
   |    1    |    1      |    | SB | 8 bit data | PB | STB |     |
   +-------------------------------------------------------------+            
        - Hardware flow control
        - Receiver/transmitter modes

  The USART_Init() function follows the USART  asynchronous configuration procedure
  (details for the procedure are available in reference manual (RM0090)).

  - For the synchronous mode in addition to the asynchronous mode parameters these 
    parameters should be also configured:
        - USART Clock Enabled
        - USART polarity
        - USART phase
        - USART LastBit
  
  These parameters can be configured using the USART_ClockInit() function.

@endverbatim
  * @{
  */
  
/**
  * @brief  Deinitializes the USARTx peripheral registers to their default reset values.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @retval None
  */
void USART_DeInit(USART_TypeDef* USARTx)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));

  if (USARTx == USART1)
  {
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_USART1, ENABLE);
    RCC_APB2PeriphResetCmd(RCC_APB2Periph_USART1, DISABLE);
  }
  else if (USARTx == USART2)
  {
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_USART2, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_USART2, DISABLE);
  }
  else if (USARTx == USART3)
  {
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_USART3, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_USART3, DISABLE);
  }    
  else if (USARTx == UART4)
  {
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_UART4, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_UART4, DISABLE);
  }
  else if (USARTx == UART5)
  {
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_UART5, ENABLE);
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_UART5, DISABLE);
  }     
  else
  {
    if (USARTx == USART6)
    { 
      RCC_APB2PeriphResetCmd(RCC_APB2Periph_USART6, ENABLE);
      RCC_APB2PeriphResetCmd(RCC_APB2Periph_USART6, DISABLE);
    }
  }
}

/**
  * @brief  Initializes the USARTx peripheral according to the specified
  *         parameters in the USART_InitStruct .
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_InitStruct: pointer to a USART_InitTypeDef structure that contains
  *         the configuration information for the specified USART peripheral.
  * @retval None
  */
void USART_Init(USART_TypeDef* USARTx, USART_InitTypeDef* USART_InitStruct)
{
  uint32_t tmpreg = 0x00, apbclock = 0x00;
  uint32_t integerdivider = 0x00;
  uint32_t fractionaldivider = 0x00;
  RCC_ClocksTypeDef RCC_ClocksStatus;

  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_BAUDRATE(USART_InitStruct->USART_BaudRate));  
  assert_param(IS_USART_WORD_LENGTH(USART_InitStruct->USART_WordLength));
  assert_param(IS_USART_STOPBITS(USART_InitStruct->USART_StopBits));
  assert_param(IS_USART_PARITY(USART_InitStruct->USART_Parity));
  assert_param(IS_USART_MODE(USART_InitStruct->USART_Mode));
  assert_param(IS_USART_HARDWARE_FLOW_CONTROL(USART_InitStruct->USART_HardwareFlowControl));

  /* The hardware flow control is available only for USART1, USART2, USART3 and USART6 */
  if (USART_InitStruct->USART_HardwareFlowControl != USART_HardwareFlowControl_None)
  {
    assert_param(IS_USART_1236_PERIPH(USARTx));
  }

/*---------------------------- USART CR2 Configuration -----------------------*/
  tmpreg = USARTx->CR2;

  /* Clear STOP[13:12] bits */
  tmpreg &= (uint32_t)~((uint32_t)USART_CR2_STOP);

  /* Configure the USART Stop Bits, Clock, CPOL, CPHA and LastBit :
      Set STOP[13:12] bits according to USART_StopBits value */
  tmpreg |= (uint32_t)USART_InitStruct->USART_StopBits;
  
  /* Write to USART CR2 */
  USARTx->CR2 = (uint16_t)tmpreg;

/*---------------------------- USART CR1 Configuration -----------------------*/
  tmpreg = USARTx->CR1;

  /* Clear M, PCE, PS, TE and RE bits */
  tmpreg &= (uint32_t)~((uint32_t)CR1_CLEAR_MASK);

  /* Configure the USART Word Length, Parity and mode: 
     Set the M bits according to USART_WordLength value 
     Set PCE and PS bits according to USART_Parity value
     Set TE and RE bits according to USART_Mode value */
  tmpreg |= (uint32_t)USART_InitStruct->USART_WordLength | USART_InitStruct->USART_Parity |
            USART_InitStruct->USART_Mode;

  /* Write to USART CR1 */
  USARTx->CR1 = (uint16_t)tmpreg;

/*---------------------------- USART CR3 Configuration -----------------------*/  
  tmpreg = USARTx->CR3;

  /* Clear CTSE and RTSE bits */
  tmpreg &= (uint32_t)~((uint32_t)CR3_CLEAR_MASK);

  /* Configure the USART HFC : 
      Set CTSE and RTSE bits according to USART_HardwareFlowControl value */
  tmpreg |= USART_InitStruct->USART_HardwareFlowControl;

  /* Write to USART CR3 */
  USARTx->CR3 = (uint16_t)tmpreg;

/*---------------------------- USART BRR Configuration -----------------------*/
  /* Configure the USART Baud Rate */
  RCC_GetClocksFreq(&RCC_ClocksStatus);

  if ((USARTx == USART1) || (USARTx == USART6))
  {
    apbclock = RCC_ClocksStatus.PCLK2_Frequency;
  }
  else
  {
    apbclock = RCC_ClocksStatus.PCLK1_Frequency;
  }
  
  /* Determine the integer part */
  if ((USARTx->CR1 & USART_CR1_OVER8) != 0)
  {
    /* Integer part computing in case Oversampling mode is 8 Samples */
    integerdivider = ((25 * apbclock) / (2 * (USART_InitStruct->USART_BaudRate)));    
  }
  else /* if ((USARTx->CR1 & USART_CR1_OVER8) == 0) */
  {
    /* Integer part computing in case Oversampling mode is 16 Samples */
    integerdivider = ((25 * apbclock) / (4 * (USART_InitStruct->USART_BaudRate)));    
  }
  tmpreg = (integerdivider / 100) << 4;

  /* Determine the fractional part */
  fractionaldivider = integerdivider - (100 * (tmpreg >> 4));

  /* Implement the fractional part in the register */
  if ((USARTx->CR1 & USART_CR1_OVER8) != 0)
  {
    tmpreg |= ((((fractionaldivider * 8) + 50) / 100)) & ((uint8_t)0x07);
  }
  else /* if ((USARTx->CR1 & USART_CR1_OVER8) == 0) */
  {
    tmpreg |= ((((fractionaldivider * 16) + 50) / 100)) & ((uint8_t)0x0F);
  }
  
  /* Write to USART BRR register */
  USARTx->BRR = (uint16_t)tmpreg;
}

/**
  * @brief  Fills each USART_InitStruct member with its default value.
  * @param  USART_InitStruct: pointer to a USART_InitTypeDef structure which will
  *         be initialized.
  * @retval None
  */
void USART_StructInit(USART_InitTypeDef* USART_InitStruct)
{
  /* USART_InitStruct members default value */
  USART_InitStruct->USART_BaudRate = 9600;
  USART_InitStruct->USART_WordLength = USART_WordLength_8b;
  USART_InitStruct->USART_StopBits = USART_StopBits_1;
  USART_InitStruct->USART_Parity = USART_Parity_No ;
  USART_InitStruct->USART_Mode = USART_Mode_Rx | USART_Mode_Tx;
  USART_InitStruct->USART_HardwareFlowControl = USART_HardwareFlowControl_None;  
}

/**
  * @brief  Initializes the USARTx peripheral Clock according to the 
  *         specified parameters in the USART_ClockInitStruct .
  * @param  USARTx: where x can be 1, 2, 3 or 6 to select the USART peripheral.
  * @param  USART_ClockInitStruct: pointer to a USART_ClockInitTypeDef structure that
  *         contains the configuration information for the specified  USART peripheral.
  * @note   The Smart Card and Synchronous modes are not available for UART4 and UART5.    
  * @retval None
  */
void USART_ClockInit(USART_TypeDef* USARTx, USART_ClockInitTypeDef* USART_ClockInitStruct)
{
  uint32_t tmpreg = 0x00;
  /* Check the parameters */
  assert_param(IS_USART_1236_PERIPH(USARTx));
  assert_param(IS_USART_CLOCK(USART_ClockInitStruct->USART_Clock));
  assert_param(IS_USART_CPOL(USART_ClockInitStruct->USART_CPOL));
  assert_param(IS_USART_CPHA(USART_ClockInitStruct->USART_CPHA));
  assert_param(IS_USART_LASTBIT(USART_ClockInitStruct->USART_LastBit));
  
/*---------------------------- USART CR2 Configuration -----------------------*/
  tmpreg = USARTx->CR2;
  /* Clear CLKEN, CPOL, CPHA and LBCL bits */
  tmpreg &= (uint32_t)~((uint32_t)CR2_CLOCK_CLEAR_MASK);
  /* Configure the USART Clock, CPOL, CPHA and LastBit ------------*/
  /* Set CLKEN bit according to USART_Clock value */
  /* Set CPOL bit according to USART_CPOL value */
  /* Set CPHA bit according to USART_CPHA value */
  /* Set LBCL bit according to USART_LastBit value */
  tmpreg |= (uint32_t)USART_ClockInitStruct->USART_Clock | USART_ClockInitStruct->USART_CPOL | 
                 USART_ClockInitStruct->USART_CPHA | USART_ClockInitStruct->USART_LastBit;
  /* Write to USART CR2 */
  USARTx->CR2 = (uint16_t)tmpreg;
}

/**
  * @brief  Fills each USART_ClockInitStruct member with its default value.
  * @param  USART_ClockInitStruct: pointer to a USART_ClockInitTypeDef structure
  *         which will be initialized.
  * @retval None
  */
void USART_ClockStructInit(USART_ClockInitTypeDef* USART_ClockInitStruct)
{
  /* USART_ClockInitStruct members default value */
  USART_ClockInitStruct->USART_Clock = USART_Clock_Disable;
  USART_ClockInitStruct->USART_CPOL = USART_CPOL_Low;
  USART_ClockInitStruct->USART_CPHA = USART_CPHA_1Edge;
  USART_ClockInitStruct->USART_LastBit = USART_LastBit_Disable;
}

/**
  * @brief  Enables or disables the specified USART peripheral.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the USARTx peripheral.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void USART_Cmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the selected USART by setting the UE bit in the CR1 register */
    USARTx->CR1 |= USART_CR1_UE;
  }
  else
  {
    /* Disable the selected USART by clearing the UE bit in the CR1 register */
    USARTx->CR1 &= (uint16_t)~((uint16_t)USART_CR1_UE);
  }
}

/**
  * @brief  Sets the system clock prescaler.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_Prescaler: specifies the prescaler clock. 
  * @note   The function is used for IrDA mode with UART4 and UART5.   
  * @retval None
  */
void USART_SetPrescaler(USART_TypeDef* USARTx, uint8_t USART_Prescaler)
{ 
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  
  /* Clear the USART prescaler */
  USARTx->GTPR &= USART_GTPR_GT;
  /* Set the USART prescaler */
  USARTx->GTPR |= USART_Prescaler;
}

/**
  * @brief  Enables or disables the USART's 8x oversampling mode.
  * @note   This function has to be called before calling USART_Init() function
  *         in order to have correct baudrate Divider value.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the USART 8x oversampling mode.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void USART_OverSampling8Cmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the 8x Oversampling mode by setting the OVER8 bit in the CR1 register */
    USARTx->CR1 |= USART_CR1_OVER8;
  }
  else
  {
    /* Disable the 8x Oversampling mode by clearing the OVER8 bit in the CR1 register */
    USARTx->CR1 &= (uint16_t)~((uint16_t)USART_CR1_OVER8);
  }
}  

/**
  * @brief  Enables or disables the USART's one bit sampling method.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the USART one bit sampling method.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void USART_OneBitMethodCmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the one bit method by setting the ONEBITE bit in the CR3 register */
    USARTx->CR3 |= USART_CR3_ONEBIT;
  }
  else
  {
    /* Disable the one bit method by clearing the ONEBITE bit in the CR3 register */
    USARTx->CR3 &= (uint16_t)~((uint16_t)USART_CR3_ONEBIT);
  }
}

/**
  * @}
  */

/** @defgroup USART_Group2 Data transfers functions
 *  @brief   Data transfers functions 
 *
@verbatim   
 ===============================================================================
                            Data transfers functions
 ===============================================================================  

  This subsection provides a set of functions allowing to manage the USART data 
  transfers.
  
  During an USART reception, data shifts in least significant bit first through 
  the RX pin. In this mode, the USART_DR register consists of a buffer (RDR) 
  between the internal bus and the received shift register.

  When a transmission is taking place, a write instruction to the USART_DR register 
  stores the data in the TDR register and which is copied in the shift register 
  at the end of the current transmission.

  The read access of the USART_DR register can be done using the USART_ReceiveData()
  function and returns the RDR buffered value. Whereas a write access to the USART_DR 
  can be done using USART_SendData() function and stores the written data into 
  TDR buffer.

@endverbatim
  * @{
  */

/**
  * @brief  Transmits single data through the USARTx peripheral.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  Data: the data to transmit.
  * @retval None
  */
void USART_SendData(USART_TypeDef* USARTx, uint16_t Data)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_DATA(Data)); 
    
  /* Transmit Data */
  USARTx->DR = (Data & (uint16_t)0x01FF);
}

/**
  * @brief  Returns the most recent received data by the USARTx peripheral.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @retval The received data.
  */
uint16_t USART_ReceiveData(USART_TypeDef* USARTx)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  
  /* Receive Data */
  return (uint16_t)(USARTx->DR & (uint16_t)0x01FF);
}

/**
  * @}
  */

/** @defgroup USART_Group3 MultiProcessor Communication functions
 *  @brief   Multi-Processor Communication functions 
 *
@verbatim   
 ===============================================================================
                    Multi-Processor Communication functions
 ===============================================================================  

  This subsection provides a set of functions allowing to manage the USART 
  multiprocessor communication.
  
  For instance one of the USARTs can be the master, its TX output is connected to 
  the RX input of the other USART. The others are slaves, their respective TX outputs 
  are logically ANDed together and connected to the RX input of the master.

  USART multiprocessor communication is possible through the following procedure:
     1. Program the Baud rate, Word length = 9 bits, Stop bits, Parity, Mode transmitter 
        or Mode receiver and hardware flow control values using the USART_Init()
        function.
     2. Configures the USART address using the USART_SetAddress() function.
     3. Configures the wake up method (USART_WakeUp_IdleLine or USART_WakeUp_AddressMark)
        using USART_WakeUpConfig() function only for the slaves.
     4. Enable the USART using the USART_Cmd() function.
     5. Enter the USART slaves in mute mode using USART_ReceiverWakeUpCmd() function.

  The USART Slave exit from mute mode when receive the wake up condition.

@endverbatim
  * @{
  */

/**
  * @brief  Sets the address of the USART node.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_Address: Indicates the address of the USART node.
  * @retval None
  */
void USART_SetAddress(USART_TypeDef* USARTx, uint8_t USART_Address)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_ADDRESS(USART_Address)); 
    
  /* Clear the USART address */
  USARTx->CR2 &= (uint16_t)~((uint16_t)USART_CR2_ADD);
  /* Set the USART address node */
  USARTx->CR2 |= USART_Address;
}

/**
  * @brief  Determines if the USART is in mute mode or not.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the USART mute mode.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void USART_ReceiverWakeUpCmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_FUNCTIONAL_STATE(NewState)); 
  
  if (NewState != DISABLE)
  {
    /* Enable the USART mute mode  by setting the RWU bit in the CR1 register */
    USARTx->CR1 |= USART_CR1_RWU;
  }
  else
  {
    /* Disable the USART mute mode by clearing the RWU bit in the CR1 register */
    USARTx->CR1 &= (uint16_t)~((uint16_t)USART_CR1_RWU);
  }
}
/**
  * @brief  Selects the USART WakeUp method.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_WakeUp: specifies the USART wakeup method.
  *          This parameter can be one of the following values:
  *            @arg USART_WakeUp_IdleLine: WakeUp by an idle line detection
  *            @arg USART_WakeUp_AddressMark: WakeUp by an address mark
  * @retval None
  */
void USART_WakeUpConfig(USART_TypeDef* USARTx, uint16_t USART_WakeUp)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_WAKEUP(USART_WakeUp));
  
  USARTx->CR1 &= (uint16_t)~((uint16_t)USART_CR1_WAKE);
  USARTx->CR1 |= USART_WakeUp;
}

/**
  * @}
  */

/** @defgroup USART_Group4 LIN mode functions
 *  @brief   LIN mode functions 
 *
@verbatim   
 ===============================================================================
                                LIN mode functions
 ===============================================================================  

  This subsection provides a set of functions allowing to manage the USART LIN 
  Mode communication.
  
  In LIN mode, 8-bit data format with 1 stop bit is required in accordance with 
  the LIN standard.

  Only this LIN Feature is supported by the USART IP:
    - LIN Master Synchronous Break send capability and LIN slave break detection
      capability :  13-bit break generation and 10/11 bit break detection


  USART LIN Master transmitter communication is possible through the following procedure:
     1. Program the Baud rate, Word length = 8bits, Stop bits = 1bit, Parity, 
        Mode transmitter or Mode receiver and hardware flow control values using 
        the USART_Init() function.
     2. Enable the USART using the USART_Cmd() function.
     3. Enable the LIN mode using the USART_LINCmd() function.
     4. Send the break character using USART_SendBreak() function.

  USART LIN Master receiver communication is possible through the following procedure:
     1. Program the Baud rate, Word length = 8bits, Stop bits = 1bit, Parity, 
        Mode transmitter or Mode receiver and hardware flow control values using 
        the USART_Init() function.
     2. Enable the USART using the USART_Cmd() function.
     3. Configures the break detection length using the USART_LINBreakDetectLengthConfig()
        function.
     4. Enable the LIN mode using the USART_LINCmd() function.


@note In LIN mode, the following bits must be kept cleared:
        - CLKEN in the USART_CR2 register,
        - STOP[1:0], SCEN, HDSEL and IREN in the USART_CR3 register.

@endverbatim
  * @{
  */

/**
  * @brief  Sets the USART LIN Break detection length.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_LINBreakDetectLength: specifies the LIN break detection length.
  *          This parameter can be one of the following values:
  *            @arg USART_LINBreakDetectLength_10b: 10-bit break detection
  *            @arg USART_LINBreakDetectLength_11b: 11-bit break detection
  * @retval None
  */
void USART_LINBreakDetectLengthConfig(USART_TypeDef* USARTx, uint16_t USART_LINBreakDetectLength)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_LIN_BREAK_DETECT_LENGTH(USART_LINBreakDetectLength));
  
  USARTx->CR2 &= (uint16_t)~((uint16_t)USART_CR2_LBDL);
  USARTx->CR2 |= USART_LINBreakDetectLength;  
}

/**
  * @brief  Enables or disables the USART's LIN mode.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the USART LIN mode.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void USART_LINCmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the LIN mode by setting the LINEN bit in the CR2 register */
    USARTx->CR2 |= USART_CR2_LINEN;
  }
  else
  {
    /* Disable the LIN mode by clearing the LINEN bit in the CR2 register */
    USARTx->CR2 &= (uint16_t)~((uint16_t)USART_CR2_LINEN);
  }
}

/**
  * @brief  Transmits break characters.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @retval None
  */
void USART_SendBreak(USART_TypeDef* USARTx)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  
  /* Send break characters */
  USARTx->CR1 |= USART_CR1_SBK;
}

/**
  * @}
  */

/** @defgroup USART_Group5 Halfduplex mode function
 *  @brief   Half-duplex mode function 
 *
@verbatim   
 ===============================================================================
                         Half-duplex mode function
 ===============================================================================  

  This subsection provides a set of functions allowing to manage the USART 
  Half-duplex communication.
  
  The USART can be configured to follow a single-wire half-duplex protocol where 
  the TX and RX lines are internally connected.

  USART Half duplex communication is possible through the following procedure:
     1. Program the Baud rate, Word length, Stop bits, Parity, Mode transmitter 
        or Mode receiver and hardware flow control values using the USART_Init()
        function.
     2. Configures the USART address using the USART_SetAddress() function.
     3. Enable the USART using the USART_Cmd() function.
     4. Enable the half duplex mode using USART_HalfDuplexCmd() function.


@note The RX pin is no longer used
@note In Half-duplex mode the following bits must be kept cleared:
        - LINEN and CLKEN bits in the USART_CR2 register.
        - SCEN and IREN bits in the USART_CR3 register.

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the USART's Half Duplex communication.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the USART Communication.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void USART_HalfDuplexCmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the Half-Duplex mode by setting the HDSEL bit in the CR3 register */
    USARTx->CR3 |= USART_CR3_HDSEL;
  }
  else
  {
    /* Disable the Half-Duplex mode by clearing the HDSEL bit in the CR3 register */
    USARTx->CR3 &= (uint16_t)~((uint16_t)USART_CR3_HDSEL);
  }
}

/**
  * @}
  */


/** @defgroup USART_Group6 Smartcard mode functions
 *  @brief   Smartcard mode functions 
 *
@verbatim   
 ===============================================================================
                               Smartcard mode functions
 ===============================================================================  

  This subsection provides a set of functions allowing to manage the USART 
  Smartcard communication.
  
  The Smartcard interface is designed to support asynchronous protocol Smartcards as
  defined in the ISO 7816-3 standard.

  The USART can provide a clock to the smartcard through the SCLK output.
  In smartcard mode, SCLK is not associated to the communication but is simply derived 
  from the internal peripheral input clock through a 5-bit prescaler.

  Smartcard communication is possible through the following procedure:
     1. Configures the Smartcard Prescaler using the USART_SetPrescaler() function.
     2. Configures the Smartcard Guard Time using the USART_SetGuardTime() function.
     3. Program the USART clock using the USART_ClockInit() function as following:
        - USART Clock enabled
        - USART CPOL Low
        - USART CPHA on first edge
        - USART Last Bit Clock Enabled
     4. Program the Smartcard interface using the USART_Init() function as following:
        - Word Length = 9 Bits
        - 1.5 Stop Bit
        - Even parity
        - BaudRate = 12096 baud
        - Hardware flow control disabled (RTS and CTS signals)
        - Tx and Rx enabled
     5. Optionally you can enable the parity error interrupt using the USART_ITConfig()
        function
     6. Enable the USART using the USART_Cmd() function.
     7. Enable the Smartcard NACK using the USART_SmartCardNACKCmd() function.
     8. Enable the Smartcard interface using the USART_SmartCardCmd() function.

  Please refer to the ISO 7816-3 specification for more details.


@note It is also possible to choose 0.5 stop bit for receiving but it is recommended 
      to use 1.5 stop bits for both transmitting and receiving to avoid switching 
      between the two configurations.
@note In smartcard mode, the following bits must be kept cleared:
        - LINEN bit in the USART_CR2 register.
        - HDSEL and IREN bits in the USART_CR3 register.
@note Smartcard mode is available on USART peripherals only (not available on UART4 
      and UART5 peripherals).

@endverbatim
  * @{
  */

/**
  * @brief  Sets the specified USART guard time.
  * @param  USARTx: where x can be 1, 2, 3 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_GuardTime: specifies the guard time.   
  * @retval None
  */
void USART_SetGuardTime(USART_TypeDef* USARTx, uint8_t USART_GuardTime)
{    
  /* Check the parameters */
  assert_param(IS_USART_1236_PERIPH(USARTx));
  
  /* Clear the USART Guard time */
  USARTx->GTPR &= USART_GTPR_PSC;
  /* Set the USART guard time */
  USARTx->GTPR |= (uint16_t)((uint16_t)USART_GuardTime << 0x08);
}

/**
  * @brief  Enables or disables the USART's Smart Card mode.
  * @param  USARTx: where x can be 1, 2, 3 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the Smart Card mode.
  *          This parameter can be: ENABLE or DISABLE.      
  * @retval None
  */
void USART_SmartCardCmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_1236_PERIPH(USARTx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the SC mode by setting the SCEN bit in the CR3 register */
    USARTx->CR3 |= USART_CR3_SCEN;
  }
  else
  {
    /* Disable the SC mode by clearing the SCEN bit in the CR3 register */
    USARTx->CR3 &= (uint16_t)~((uint16_t)USART_CR3_SCEN);
  }
}

/**
  * @brief  Enables or disables NACK transmission.
  * @param  USARTx: where x can be 1, 2, 3 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the NACK transmission.
  *          This parameter can be: ENABLE or DISABLE.  
  * @retval None
  */
void USART_SmartCardNACKCmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_1236_PERIPH(USARTx)); 
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the NACK transmission by setting the NACK bit in the CR3 register */
    USARTx->CR3 |= USART_CR3_NACK;
  }
  else
  {
    /* Disable the NACK transmission by clearing the NACK bit in the CR3 register */
    USARTx->CR3 &= (uint16_t)~((uint16_t)USART_CR3_NACK);
  }
}

/**
  * @}
  */

/** @defgroup USART_Group7 IrDA mode functions
 *  @brief   IrDA mode functions 
 *
@verbatim   
 ===============================================================================
                                IrDA mode functions
 ===============================================================================  

  This subsection provides a set of functions allowing to manage the USART 
  IrDA communication.
  
  IrDA is a half duplex communication protocol. If the Transmitter is busy, any data
  on the IrDA receive line will be ignored by the IrDA decoder and if the Receiver 
  is busy, data on the TX from the USART to IrDA will not be encoded by IrDA.
  While receiving data, transmission should be avoided as the data to be transmitted
  could be corrupted.

  IrDA communication is possible through the following procedure:
     1. Program the Baud rate, Word length = 8 bits, Stop bits, Parity, Transmitter/Receiver 
        modes and hardware flow control values using the USART_Init() function.
     2. Enable the USART using the USART_Cmd() function.
     3. Configures the IrDA pulse width by configuring the prescaler using  
        the USART_SetPrescaler() function.
     4. Configures the IrDA  USART_IrDAMode_LowPower or USART_IrDAMode_Normal mode
        using the USART_IrDAConfig() function.
     5. Enable the IrDA using the USART_IrDACmd() function.

@note A pulse of width less than two and greater than one PSC period(s) may or may
      not be rejected.
@note The receiver set up time should be managed by software. The IrDA physical layer
      specification specifies a minimum of 10 ms delay between transmission and 
      reception (IrDA is a half duplex protocol).
@note In IrDA mode, the following bits must be kept cleared:
        - LINEN, STOP and CLKEN bits in the USART_CR2 register.
        - SCEN and HDSEL bits in the USART_CR3 register.

@endverbatim
  * @{
  */

/**
  * @brief  Configures the USART's IrDA interface.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_IrDAMode: specifies the IrDA mode.
  *          This parameter can be one of the following values:
  *            @arg USART_IrDAMode_LowPower
  *            @arg USART_IrDAMode_Normal
  * @retval None
  */
void USART_IrDAConfig(USART_TypeDef* USARTx, uint16_t USART_IrDAMode)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_IRDA_MODE(USART_IrDAMode));
    
  USARTx->CR3 &= (uint16_t)~((uint16_t)USART_CR3_IRLP);
  USARTx->CR3 |= USART_IrDAMode;
}

/**
  * @brief  Enables or disables the USART's IrDA interface.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  NewState: new state of the IrDA mode.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void USART_IrDACmd(USART_TypeDef* USARTx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
    
  if (NewState != DISABLE)
  {
    /* Enable the IrDA mode by setting the IREN bit in the CR3 register */
    USARTx->CR3 |= USART_CR3_IREN;
  }
  else
  {
    /* Disable the IrDA mode by clearing the IREN bit in the CR3 register */
    USARTx->CR3 &= (uint16_t)~((uint16_t)USART_CR3_IREN);
  }
}

/**
  * @}
  */

/** @defgroup USART_Group8 DMA transfers management functions
 *  @brief   DMA transfers management functions
 *
@verbatim   
 ===============================================================================
                      DMA transfers management functions
 ===============================================================================  

@endverbatim
  * @{
  */
  
/**
  * @brief  Enables or disables the USART's DMA interface.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_DMAReq: specifies the DMA request.
  *          This parameter can be any combination of the following values:
  *            @arg USART_DMAReq_Tx: USART DMA transmit request
  *            @arg USART_DMAReq_Rx: USART DMA receive request
  * @param  NewState: new state of the DMA Request sources.
  *          This parameter can be: ENABLE or DISABLE.   
  * @retval None
  */
void USART_DMACmd(USART_TypeDef* USARTx, uint16_t USART_DMAReq, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_DMAREQ(USART_DMAReq));  
  assert_param(IS_FUNCTIONAL_STATE(NewState)); 

  if (NewState != DISABLE)
  {
    /* Enable the DMA transfer for selected requests by setting the DMAT and/or
       DMAR bits in the USART CR3 register */
    USARTx->CR3 |= USART_DMAReq;
  }
  else
  {
    /* Disable the DMA transfer for selected requests by clearing the DMAT and/or
       DMAR bits in the USART CR3 register */
    USARTx->CR3 &= (uint16_t)~USART_DMAReq;
  }
}

/**
  * @}
  */
  
/** @defgroup USART_Group9 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions 
 *
@verbatim   
 ===============================================================================
                   Interrupts and flags management functions
 ===============================================================================  

  This subsection provides a set of functions allowing to configure the USART 
  Interrupts sources, DMA channels requests and check or clear the flags or 
  pending bits status.
  The user should identify which mode will be used in his application to manage 
  the communication: Polling mode, Interrupt mode or DMA mode. 
    
  Polling Mode
  =============
  In Polling Mode, the SPI communication can be managed by 10 flags:
     1. USART_FLAG_TXE : to indicate the status of the transmit buffer register
     2. USART_FLAG_RXNE : to indicate the status of the receive buffer register
     3. USART_FLAG_TC : to indicate the status of the transmit operation
     4. USART_FLAG_IDLE : to indicate the status of the Idle Line             
     5. USART_FLAG_CTS : to indicate the status of the nCTS input
     6. USART_FLAG_LBD : to indicate the status of the LIN break detection
     7. USART_FLAG_NE : to indicate if a noise error occur
     8. USART_FLAG_FE : to indicate if a frame error occur
     9. USART_FLAG_PE : to indicate if a parity error occur
     10. USART_FLAG_ORE : to indicate if an Overrun error occur

  In this Mode it is advised to use the following functions:
      - FlagStatus USART_GetFlagStatus(USART_TypeDef* USARTx, uint16_t USART_FLAG);
      - void USART_ClearFlag(USART_TypeDef* USARTx, uint16_t USART_FLAG);

  Interrupt Mode
  ===============
  In Interrupt Mode, the USART communication can be managed by 8 interrupt sources
  and 10 pending bits: 

  Pending Bits:
  ------------- 
     1. USART_IT_TXE : to indicate the status of the transmit buffer register
     2. USART_IT_RXNE : to indicate the status of the receive buffer register
     3. USART_IT_TC : to indicate the status of the transmit operation
     4. USART_IT_IDLE : to indicate the status of the Idle Line             
     5. USART_IT_CTS : to indicate the status of the nCTS input
     6. USART_IT_LBD : to indicate the status of the LIN break detection
     7. USART_IT_NE : to indicate if a noise error occur
     8. USART_IT_FE : to indicate if a frame error occur
     9. USART_IT_PE : to indicate if a parity error occur
     10. USART_IT_ORE : to indicate if an Overrun error occur

  Interrupt Source:
  -----------------
     1. USART_IT_TXE : specifies the interrupt source for the Tx buffer empty 
                       interrupt. 
     2. USART_IT_RXNE : specifies the interrupt source for the Rx buffer not 
                        empty interrupt.
     3. USART_IT_TC : specifies the interrupt source for the Transmit complete 
                       interrupt. 
     4. USART_IT_IDLE : specifies the interrupt source for the Idle Line interrupt.             
     5. USART_IT_CTS : specifies the interrupt source for the CTS interrupt. 
     6. USART_IT_LBD : specifies the interrupt source for the LIN break detection
                       interrupt. 
     7. USART_IT_PE : specifies the interrupt source for the parity error interrupt. 
     8. USART_IT_ERR :  specifies the interrupt source for the errors interrupt.

@note Some parameters are coded in order to use them as interrupt source or as pending bits.

  In this Mode it is advised to use the following functions:
     - void USART_ITConfig(USART_TypeDef* USARTx, uint16_t USART_IT, FunctionalState NewState);
     - ITStatus USART_GetITStatus(USART_TypeDef* USARTx, uint16_t USART_IT);
     - void USART_ClearITPendingBit(USART_TypeDef* USARTx, uint16_t USART_IT);

  DMA Mode
  ========
  In DMA Mode, the USART communication can be managed by 2 DMA Channel requests:
     1. USART_DMAReq_Tx: specifies the Tx buffer DMA transfer request
     2. USART_DMAReq_Rx: specifies the Rx buffer DMA transfer request

  In this Mode it is advised to use the following function:
     - void USART_DMACmd(USART_TypeDef* USARTx, uint16_t USART_DMAReq, FunctionalState NewState);

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the specified USART interrupts.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_IT: specifies the USART interrupt sources to be enabled or disabled.
  *          This parameter can be one of the following values:
  *            @arg USART_IT_CTS:  CTS change interrupt
  *            @arg USART_IT_LBD:  LIN Break detection interrupt
  *            @arg USART_IT_TXE:  Transmit Data Register empty interrupt
  *            @arg USART_IT_TC:   Transmission complete interrupt
  *            @arg USART_IT_RXNE: Receive Data register not empty interrupt
  *            @arg USART_IT_IDLE: Idle line detection interrupt
  *            @arg USART_IT_PE:   Parity Error interrupt
  *            @arg USART_IT_ERR:  Error interrupt(Frame error, noise error, overrun error)
  * @param  NewState: new state of the specified USARTx interrupts.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void USART_ITConfig(USART_TypeDef* USARTx, uint16_t USART_IT, FunctionalState NewState)
{
  uint32_t usartreg = 0x00, itpos = 0x00, itmask = 0x00;
  uint32_t usartxbase = 0x00;
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_CONFIG_IT(USART_IT));
  assert_param(IS_FUNCTIONAL_STATE(NewState));

  /* The CTS interrupt is not available for UART4 and UART5 */
  if (USART_IT == USART_IT_CTS)
  {
    assert_param(IS_USART_1236_PERIPH(USARTx));
  } 
    
  usartxbase = (uint32_t)USARTx;

  /* Get the USART register index */
  usartreg = (((uint8_t)USART_IT) >> 0x05);

  /* Get the interrupt position */
  itpos = USART_IT & IT_MASK;
  itmask = (((uint32_t)0x01) << itpos);
    
  if (usartreg == 0x01) /* The IT is in CR1 register */
  {
    usartxbase += 0x0C;
  }
  else if (usartreg == 0x02) /* The IT is in CR2 register */
  {
    usartxbase += 0x10;
  }
  else /* The IT is in CR3 register */
  {
    usartxbase += 0x14; 
  }
  if (NewState != DISABLE)
  {
    *(__IO uint32_t*)usartxbase  |= itmask;
  }
  else
  {
    *(__IO uint32_t*)usartxbase &= ~itmask;
  }
}

/**
  * @brief  Checks whether the specified USART flag is set or not.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_FLAG: specifies the flag to check.
  *          This parameter can be one of the following values:
  *            @arg USART_FLAG_CTS:  CTS Change flag (not available for UART4 and UART5)
  *            @arg USART_FLAG_LBD:  LIN Break detection flag
  *            @arg USART_FLAG_TXE:  Transmit data register empty flag
  *            @arg USART_FLAG_TC:   Transmission Complete flag
  *            @arg USART_FLAG_RXNE: Receive data register not empty flag
  *            @arg USART_FLAG_IDLE: Idle Line detection flag
  *            @arg USART_FLAG_ORE:  OverRun Error flag
  *            @arg USART_FLAG_NE:   Noise Error flag
  *            @arg USART_FLAG_FE:   Framing Error flag
  *            @arg USART_FLAG_PE:   Parity Error flag
  * @retval The new state of USART_FLAG (SET or RESET).
  */
FlagStatus USART_GetFlagStatus(USART_TypeDef* USARTx, uint16_t USART_FLAG)
{
  FlagStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_FLAG(USART_FLAG));

  /* The CTS flag is not available for UART4 and UART5 */
  if (USART_FLAG == USART_FLAG_CTS)
  {
    assert_param(IS_USART_1236_PERIPH(USARTx));
  } 
    
  if ((USARTx->SR & USART_FLAG) != (uint16_t)RESET)
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  return bitstatus;
}

/**
  * @brief  Clears the USARTx's pending flags.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_FLAG: specifies the flag to clear.
  *          This parameter can be any combination of the following values:
  *            @arg USART_FLAG_CTS:  CTS Change flag (not available for UART4 and UART5).
  *            @arg USART_FLAG_LBD:  LIN Break detection flag.
  *            @arg USART_FLAG_TC:   Transmission Complete flag.
  *            @arg USART_FLAG_RXNE: Receive data register not empty flag.
  *   
  * @note   PE (Parity error), FE (Framing error), NE (Noise error), ORE (OverRun 
  *          error) and IDLE (Idle line detected) flags are cleared by software 
  *          sequence: a read operation to USART_SR register (USART_GetFlagStatus()) 
  *          followed by a read operation to USART_DR register (USART_ReceiveData()).
  * @note   RXNE flag can be also cleared by a read to the USART_DR register 
  *          (USART_ReceiveData()).
  * @note   TC flag can be also cleared by software sequence: a read operation to 
  *          USART_SR register (USART_GetFlagStatus()) followed by a write operation
  *          to USART_DR register (USART_SendData()).
  * @note   TXE flag is cleared only by a write to the USART_DR register 
  *          (USART_SendData()).
  *   
  * @retval None
  */
void USART_ClearFlag(USART_TypeDef* USARTx, uint16_t USART_FLAG)
{
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_CLEAR_FLAG(USART_FLAG));

  /* The CTS flag is not available for UART4 and UART5 */
  if ((USART_FLAG & USART_FLAG_CTS) == USART_FLAG_CTS)
  {
    assert_param(IS_USART_1236_PERIPH(USARTx));
  } 
       
  USARTx->SR = (uint16_t)~USART_FLAG;
}

/**
  * @brief  Checks whether the specified USART interrupt has occurred or not.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_IT: specifies the USART interrupt source to check.
  *          This parameter can be one of the following values:
  *            @arg USART_IT_CTS:  CTS change interrupt (not available for UART4 and UART5)
  *            @arg USART_IT_LBD:  LIN Break detection interrupt
  *            @arg USART_IT_TXE:  Transmit Data Register empty interrupt
  *            @arg USART_IT_TC:   Transmission complete interrupt
  *            @arg USART_IT_RXNE: Receive Data register not empty interrupt
  *            @arg USART_IT_IDLE: Idle line detection interrupt
  *            @arg USART_IT_ORE_RX : OverRun Error interrupt if the RXNEIE bit is set
  *            @arg USART_IT_ORE_ER : OverRun Error interrupt if the EIE bit is set  
  *            @arg USART_IT_NE:   Noise Error interrupt
  *            @arg USART_IT_FE:   Framing Error interrupt
  *            @arg USART_IT_PE:   Parity Error interrupt
  * @retval The new state of USART_IT (SET or RESET).
  */
ITStatus USART_GetITStatus(USART_TypeDef* USARTx, uint16_t USART_IT)
{
  uint32_t bitpos = 0x00, itmask = 0x00, usartreg = 0x00;
  ITStatus bitstatus = RESET;
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_GET_IT(USART_IT)); 

  /* The CTS interrupt is not available for UART4 and UART5 */ 
  if (USART_IT == USART_IT_CTS)
  {
    assert_param(IS_USART_1236_PERIPH(USARTx));
  } 
    
  /* Get the USART register index */
  usartreg = (((uint8_t)USART_IT) >> 0x05);
  /* Get the interrupt position */
  itmask = USART_IT & IT_MASK;
  itmask = (uint32_t)0x01 << itmask;
  
  if (usartreg == 0x01) /* The IT  is in CR1 register */
  {
    itmask &= USARTx->CR1;
  }
  else if (usartreg == 0x02) /* The IT  is in CR2 register */
  {
    itmask &= USARTx->CR2;
  }
  else /* The IT  is in CR3 register */
  {
    itmask &= USARTx->CR3;
  }
  
  bitpos = USART_IT >> 0x08;
  bitpos = (uint32_t)0x01 << bitpos;
  bitpos &= USARTx->SR;
  if ((itmask != (uint16_t)RESET)&&(bitpos != (uint16_t)RESET))
  {
    bitstatus = SET;
  }
  else
  {
    bitstatus = RESET;
  }
  
  return bitstatus;  
}

/**
  * @brief  Clears the USARTx's interrupt pending bits.
  * @param  USARTx: where x can be 1, 2, 3, 4, 5 or 6 to select the USART or 
  *         UART peripheral.
  * @param  USART_IT: specifies the interrupt pending bit to clear.
  *          This parameter can be one of the following values:
  *            @arg USART_IT_CTS:  CTS change interrupt (not available for UART4 and UART5)
  *            @arg USART_IT_LBD:  LIN Break detection interrupt
  *            @arg USART_IT_TC:   Transmission complete interrupt. 
  *            @arg USART_IT_RXNE: Receive Data register not empty interrupt.
  *
  * @note   PE (Parity error), FE (Framing error), NE (Noise error), ORE (OverRun 
  *          error) and IDLE (Idle line detected) pending bits are cleared by 
  *          software sequence: a read operation to USART_SR register 
  *          (USART_GetITStatus()) followed by a read operation to USART_DR register 
  *          (USART_ReceiveData()).
  * @note   RXNE pending bit can be also cleared by a read to the USART_DR register 
  *          (USART_ReceiveData()).
  * @note   TC pending bit can be also cleared by software sequence: a read 
  *          operation to USART_SR register (USART_GetITStatus()) followed by a write 
  *          operation to USART_DR register (USART_SendData()).
  * @note   TXE pending bit is cleared only by a write to the USART_DR register 
  *          (USART_SendData()).
  *  
  * @retval None
  */
void USART_ClearITPendingBit(USART_TypeDef* USARTx, uint16_t USART_IT)
{
  uint16_t bitpos = 0x00, itmask = 0x00;
  /* Check the parameters */
  assert_param(IS_USART_ALL_PERIPH(USARTx));
  assert_param(IS_USART_CLEAR_IT(USART_IT)); 

  /* The CTS interrupt is not available for UART4 and UART5 */
  if (USART_IT == USART_IT_CTS)
  {
    assert_param(IS_USART_1236_PERIPH(USARTx));
  } 
    
  bitpos = USART_IT >> 0x08;
  itmask = ((uint16_t)0x01 << (uint16_t)bitpos);
  USARTx->SR = (uint16_t)~itmask;
}

/**
  * @}
  */

/**
  * @}
  */

/**
  * @}
  */

/**
  * @}
  */

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
