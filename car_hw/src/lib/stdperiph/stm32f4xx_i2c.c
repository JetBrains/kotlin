/**
  ******************************************************************************
  * @file    stm32f4xx_i2c.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the Inter-integrated circuit (I2C)
  *           - Initialization and Configuration
  *           - Data transfers
  *           - PEC management
  *           - DMA transfers management
  *           - Interrupts, events and flags management 
  *           
  *  @verbatim
  *    
  *          ===================================================================
  *                                 How to use this driver
  *          ===================================================================
  *          1. Enable peripheral clock using RCC_APB1PeriphClockCmd(RCC_APB1Periph_I2Cx, ENABLE)
  *             function for I2C1, I2C2 or I2C3.
  *
  *          2. Enable SDA, SCL  and SMBA (when used) GPIO clocks using 
  *             RCC_AHBPeriphClockCmd() function. 
  *
  *          3. Peripherals alternate function: 
  *                 - Connect the pin to the desired peripherals' Alternate 
  *                   Function (AF) using GPIO_PinAFConfig() function
  *                 - Configure the desired pin in alternate function by:
  *                   GPIO_InitStruct->GPIO_Mode = GPIO_Mode_AF
  *                 - Select the type, pull-up/pull-down and output speed via 
  *                   GPIO_PuPd, GPIO_OType and GPIO_Speed members
  *                 - Call GPIO_Init() function
  *                 Recommended configuration is Push-Pull, Pull-up, Open-Drain.
  *                 Add an external pull up if necessary (typically 4.7 KOhm).      
  *        
  *          4. Program the Mode, duty cycle , Own address, Ack, Speed and Acknowledged
  *             Address using the I2C_Init() function.
  *
  *          5. Optionally you can enable/configure the following parameters without
  *             re-initialization (i.e there is no need to call again I2C_Init() function):
  *              - Enable the acknowledge feature using I2C_AcknowledgeConfig() function
  *              - Enable the dual addressing mode using I2C_DualAddressCmd() function
  *              - Enable the general call using the I2C_GeneralCallCmd() function
  *              - Enable the clock stretching using I2C_StretchClockCmd() function
  *              - Enable the fast mode duty cycle using the I2C_FastModeDutyCycleConfig()
  *                function.
  *              - Configure the NACK position for Master Receiver mode in case of 
  *                2 bytes reception using the function I2C_NACKPositionConfig().  
  *              - Enable the PEC Calculation using I2C_CalculatePEC() function
  *              - For SMBus Mode: 
  *                   - Enable the Address Resolution Protocol (ARP) using I2C_ARPCmd() function
  *                   - Configure the SMBusAlert pin using I2C_SMBusAlertConfig() function
  *
  *          6. Enable the NVIC and the corresponding interrupt using the function 
  *             I2C_ITConfig() if you need to use interrupt mode. 
  *
  *          7. When using the DMA mode 
  *                   - Configure the DMA using DMA_Init() function
  *                   - Active the needed channel Request using I2C_DMACmd() or
  *                     I2C_DMALastTransferCmd() function.
  *              @note When using DMA mode, I2C interrupts may be used at the same time to
  *                    control the communication flow (Start/Stop/Ack... events and errors).
  * 
  *          8. Enable the I2C using the I2C_Cmd() function.
  * 
  *          9. Enable the DMA using the DMA_Cmd() function when using DMA mode in the 
  *             transfers. 
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
#include "stm32f4xx_i2c.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup I2C 
  * @brief I2C driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/

#define CR1_CLEAR_MASK    ((uint16_t)0xFBF5)      /*<! I2C registers Masks */
#define FLAG_MASK         ((uint32_t)0x00FFFFFF)  /*<! I2C FLAG mask */
#define ITEN_MASK         ((uint32_t)0x07000000)  /*<! I2C Interrupt Enable mask */

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup I2C_Private_Functions
  * @{
  */

/** @defgroup I2C_Group1 Initialization and Configuration functions
 *  @brief   Initialization and Configuration functions 
 *
@verbatim   
 ===============================================================================
                   Initialization and Configuration functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Deinitialize the I2Cx peripheral registers to their default reset values.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @retval None
  */
void I2C_DeInit(I2C_TypeDef* I2Cx)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));

  if (I2Cx == I2C1)
  {
    /* Enable I2C1 reset state */
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_I2C1, ENABLE);
    /* Release I2C1 from reset state */
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_I2C1, DISABLE);    
  }
  else if (I2Cx == I2C2)
  {
    /* Enable I2C2 reset state */
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_I2C2, ENABLE);
    /* Release I2C2 from reset state */
    RCC_APB1PeriphResetCmd(RCC_APB1Periph_I2C2, DISABLE);      
  }
  else 
  {
    if (I2Cx == I2C3)
    {
      /* Enable I2C3 reset state */
      RCC_APB1PeriphResetCmd(RCC_APB1Periph_I2C3, ENABLE);
      /* Release I2C3 from reset state */
      RCC_APB1PeriphResetCmd(RCC_APB1Periph_I2C3, DISABLE);     
    }
  }
}

/**
  * @brief  Initializes the I2Cx peripheral according to the specified 
  *         parameters in the I2C_InitStruct.
  *           
  * @note   To use the I2C at 400 KHz (in fast mode), the PCLK1 frequency 
  *         (I2C peripheral input clock) must be a multiple of 10 MHz.  
  *           
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_InitStruct: pointer to a I2C_InitTypeDef structure that contains 
  *         the configuration information for the specified I2C peripheral.
  * @retval None
  */
void I2C_Init(I2C_TypeDef* I2Cx, I2C_InitTypeDef* I2C_InitStruct)
{
  uint16_t tmpreg = 0, freqrange = 0;
  uint16_t result = 0x04;
  uint32_t pclk1 = 8000000;
  RCC_ClocksTypeDef  rcc_clocks;
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_CLOCK_SPEED(I2C_InitStruct->I2C_ClockSpeed));
  assert_param(IS_I2C_MODE(I2C_InitStruct->I2C_Mode));
  assert_param(IS_I2C_DUTY_CYCLE(I2C_InitStruct->I2C_DutyCycle));
  assert_param(IS_I2C_OWN_ADDRESS1(I2C_InitStruct->I2C_OwnAddress1));
  assert_param(IS_I2C_ACK_STATE(I2C_InitStruct->I2C_Ack));
  assert_param(IS_I2C_ACKNOWLEDGE_ADDRESS(I2C_InitStruct->I2C_AcknowledgedAddress));

/*---------------------------- I2Cx CR2 Configuration ------------------------*/
  /* Get the I2Cx CR2 value */
  tmpreg = I2Cx->CR2;
  /* Clear frequency FREQ[5:0] bits */
  tmpreg &= (uint16_t)~((uint16_t)I2C_CR2_FREQ);
  /* Get pclk1 frequency value */
  RCC_GetClocksFreq(&rcc_clocks);
  pclk1 = rcc_clocks.PCLK1_Frequency;
  /* Set frequency bits depending on pclk1 value */
  freqrange = (uint16_t)(pclk1 / 1000000);
  tmpreg |= freqrange;
  /* Write to I2Cx CR2 */
  I2Cx->CR2 = tmpreg;

/*---------------------------- I2Cx CCR Configuration ------------------------*/
  /* Disable the selected I2C peripheral to configure TRISE */
  I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_PE);
  /* Reset tmpreg value */
  /* Clear F/S, DUTY and CCR[11:0] bits */
  tmpreg = 0;

  /* Configure speed in standard mode */
  if (I2C_InitStruct->I2C_ClockSpeed <= 100000)
  {
    /* Standard mode speed calculate */
    result = (uint16_t)(pclk1 / (I2C_InitStruct->I2C_ClockSpeed << 1));
    /* Test if CCR value is under 0x4*/
    if (result < 0x04)
    {
      /* Set minimum allowed value */
      result = 0x04;  
    }
    /* Set speed value for standard mode */
    tmpreg |= result;	  
    /* Set Maximum Rise Time for standard mode */
    I2Cx->TRISE = freqrange + 1; 
  }
  /* Configure speed in fast mode */
  /* To use the I2C at 400 KHz (in fast mode), the PCLK1 frequency (I2C peripheral
     input clock) must be a multiple of 10 MHz */
  else /*(I2C_InitStruct->I2C_ClockSpeed <= 400000)*/
  {
    if (I2C_InitStruct->I2C_DutyCycle == I2C_DutyCycle_2)
    {
      /* Fast mode speed calculate: Tlow/Thigh = 2 */
      result = (uint16_t)(pclk1 / (I2C_InitStruct->I2C_ClockSpeed * 3));
    }
    else /*I2C_InitStruct->I2C_DutyCycle == I2C_DutyCycle_16_9*/
    {
      /* Fast mode speed calculate: Tlow/Thigh = 16/9 */
      result = (uint16_t)(pclk1 / (I2C_InitStruct->I2C_ClockSpeed * 25));
      /* Set DUTY bit */
      result |= I2C_DutyCycle_16_9;
    }

    /* Test if CCR value is under 0x1*/
    if ((result & I2C_CCR_CCR) == 0)
    {
      /* Set minimum allowed value */
      result |= (uint16_t)0x0001;  
    }
    /* Set speed value and set F/S bit for fast mode */
    tmpreg |= (uint16_t)(result | I2C_CCR_FS);
    /* Set Maximum Rise Time for fast mode */
    I2Cx->TRISE = (uint16_t)(((freqrange * (uint16_t)300) / (uint16_t)1000) + (uint16_t)1);  
  }

  /* Write to I2Cx CCR */
  I2Cx->CCR = tmpreg;
  /* Enable the selected I2C peripheral */
  I2Cx->CR1 |= I2C_CR1_PE;

/*---------------------------- I2Cx CR1 Configuration ------------------------*/
  /* Get the I2Cx CR1 value */
  tmpreg = I2Cx->CR1;
  /* Clear ACK, SMBTYPE and  SMBUS bits */
  tmpreg &= CR1_CLEAR_MASK;
  /* Configure I2Cx: mode and acknowledgement */
  /* Set SMBTYPE and SMBUS bits according to I2C_Mode value */
  /* Set ACK bit according to I2C_Ack value */
  tmpreg |= (uint16_t)((uint32_t)I2C_InitStruct->I2C_Mode | I2C_InitStruct->I2C_Ack);
  /* Write to I2Cx CR1 */
  I2Cx->CR1 = tmpreg;

/*---------------------------- I2Cx OAR1 Configuration -----------------------*/
  /* Set I2Cx Own Address1 and acknowledged address */
  I2Cx->OAR1 = (I2C_InitStruct->I2C_AcknowledgedAddress | I2C_InitStruct->I2C_OwnAddress1);
}

/**
  * @brief  Fills each I2C_InitStruct member with its default value.
  * @param  I2C_InitStruct: pointer to an I2C_InitTypeDef structure which will be initialized.
  * @retval None
  */
void I2C_StructInit(I2C_InitTypeDef* I2C_InitStruct)
{
/*---------------- Reset I2C init structure parameters values ----------------*/
  /* initialize the I2C_ClockSpeed member */
  I2C_InitStruct->I2C_ClockSpeed = 5000;
  /* Initialize the I2C_Mode member */
  I2C_InitStruct->I2C_Mode = I2C_Mode_I2C;
  /* Initialize the I2C_DutyCycle member */
  I2C_InitStruct->I2C_DutyCycle = I2C_DutyCycle_2;
  /* Initialize the I2C_OwnAddress1 member */
  I2C_InitStruct->I2C_OwnAddress1 = 0;
  /* Initialize the I2C_Ack member */
  I2C_InitStruct->I2C_Ack = I2C_Ack_Disable;
  /* Initialize the I2C_AcknowledgedAddress member */
  I2C_InitStruct->I2C_AcknowledgedAddress = I2C_AcknowledgedAddress_7bit;
}

/**
  * @brief  Enables or disables the specified I2C peripheral.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2Cx peripheral. 
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_Cmd(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected I2C peripheral */
    I2Cx->CR1 |= I2C_CR1_PE;
  }
  else
  {
    /* Disable the selected I2C peripheral */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_PE);
  }
}

/**
  * @brief  Generates I2Cx communication START condition.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C START condition generation.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None.
  */
void I2C_GenerateSTART(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Generate a START condition */
    I2Cx->CR1 |= I2C_CR1_START;
  }
  else
  {
    /* Disable the START condition generation */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_START);
  }
}

/**
  * @brief  Generates I2Cx communication STOP condition.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C STOP condition generation.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None.
  */
void I2C_GenerateSTOP(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Generate a STOP condition */
    I2Cx->CR1 |= I2C_CR1_STOP;
  }
  else
  {
    /* Disable the STOP condition generation */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_STOP);
  }
}

/**
  * @brief  Transmits the address byte to select the slave device.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  Address: specifies the slave address which will be transmitted
  * @param  I2C_Direction: specifies whether the I2C device will be a Transmitter
  *         or a Receiver. 
  *          This parameter can be one of the following values
  *            @arg I2C_Direction_Transmitter: Transmitter mode
  *            @arg I2C_Direction_Receiver: Receiver mode
  * @retval None.
  */
void I2C_Send7bitAddress(I2C_TypeDef* I2Cx, uint8_t Address, uint8_t I2C_Direction)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_DIRECTION(I2C_Direction));
  /* Test on the direction to set/reset the read/write bit */
  if (I2C_Direction != I2C_Direction_Transmitter)
  {
    /* Set the address bit0 for read */
    Address |= I2C_OAR1_ADD0;
  }
  else
  {
    /* Reset the address bit0 for write */
    Address &= (uint8_t)~((uint8_t)I2C_OAR1_ADD0);
  }
  /* Send the address */
  I2Cx->DR = Address;
}

/**
  * @brief  Enables or disables the specified I2C acknowledge feature.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C Acknowledgement.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None.
  */
void I2C_AcknowledgeConfig(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the acknowledgement */
    I2Cx->CR1 |= I2C_CR1_ACK;
  }
  else
  {
    /* Disable the acknowledgement */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_ACK);
  }
}

/**
  * @brief  Configures the specified I2C own address2.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  Address: specifies the 7bit I2C own address2.
  * @retval None.
  */
void I2C_OwnAddress2Config(I2C_TypeDef* I2Cx, uint8_t Address)
{
  uint16_t tmpreg = 0;

  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));

  /* Get the old register value */
  tmpreg = I2Cx->OAR2;

  /* Reset I2Cx Own address2 bit [7:1] */
  tmpreg &= (uint16_t)~((uint16_t)I2C_OAR2_ADD2);

  /* Set I2Cx Own address2 */
  tmpreg |= (uint16_t)((uint16_t)Address & (uint16_t)0x00FE);

  /* Store the new register value */
  I2Cx->OAR2 = tmpreg;
}

/**
  * @brief  Enables or disables the specified I2C dual addressing mode.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C dual addressing mode.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_DualAddressCmd(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable dual addressing mode */
    I2Cx->OAR2 |= I2C_OAR2_ENDUAL;
  }
  else
  {
    /* Disable dual addressing mode */
    I2Cx->OAR2 &= (uint16_t)~((uint16_t)I2C_OAR2_ENDUAL);
  }
}

/**
  * @brief  Enables or disables the specified I2C general call feature.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C General call.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_GeneralCallCmd(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable generall call */
    I2Cx->CR1 |= I2C_CR1_ENGC;
  }
  else
  {
    /* Disable generall call */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_ENGC);
  }
}

/**
  * @brief  Enables or disables the specified I2C software reset.
  * @note   When software reset is enabled, the I2C IOs are released (this can
  *         be useful to recover from bus errors).  
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C software reset.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_SoftwareResetCmd(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Peripheral under reset */
    I2Cx->CR1 |= I2C_CR1_SWRST;
  }
  else
  {
    /* Peripheral not under reset */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_SWRST);
  }
}

/**
  * @brief  Enables or disables the specified I2C Clock stretching.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2Cx Clock stretching.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_StretchClockCmd(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState == DISABLE)
  {
    /* Enable the selected I2C Clock stretching */
    I2Cx->CR1 |= I2C_CR1_NOSTRETCH;
  }
  else
  {
    /* Disable the selected I2C Clock stretching */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_NOSTRETCH);
  }
}

/**
  * @brief  Selects the specified I2C fast mode duty cycle.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_DutyCycle: specifies the fast mode duty cycle.
  *          This parameter can be one of the following values:
  *            @arg I2C_DutyCycle_2: I2C fast mode Tlow/Thigh = 2
  *            @arg I2C_DutyCycle_16_9: I2C fast mode Tlow/Thigh = 16/9
  * @retval None
  */
void I2C_FastModeDutyCycleConfig(I2C_TypeDef* I2Cx, uint16_t I2C_DutyCycle)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_DUTY_CYCLE(I2C_DutyCycle));
  if (I2C_DutyCycle != I2C_DutyCycle_16_9)
  {
    /* I2C fast mode Tlow/Thigh=2 */
    I2Cx->CCR &= I2C_DutyCycle_2;
  }
  else
  {
    /* I2C fast mode Tlow/Thigh=16/9 */
    I2Cx->CCR |= I2C_DutyCycle_16_9;
  }
}

/**
  * @brief  Selects the specified I2C NACK position in master receiver mode.
  * @note   This function is useful in I2C Master Receiver mode when the number
  *         of data to be received is equal to 2. In this case, this function 
  *         should be called (with parameter I2C_NACKPosition_Next) before data 
  *         reception starts,as described in the 2-byte reception procedure 
  *         recommended in Reference Manual in Section: Master receiver.                
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_NACKPosition: specifies the NACK position. 
  *          This parameter can be one of the following values:
  *            @arg I2C_NACKPosition_Next: indicates that the next byte will be the last
  *                                        received byte.  
  *            @arg I2C_NACKPosition_Current: indicates that current byte is the last 
  *                                           received byte.
  *            
  * @note    This function configures the same bit (POS) as I2C_PECPositionConfig() 
  *          but is intended to be used in I2C mode while I2C_PECPositionConfig() 
  *          is intended to used in SMBUS mode. 
  *            
  * @retval None
  */
void I2C_NACKPositionConfig(I2C_TypeDef* I2Cx, uint16_t I2C_NACKPosition)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_NACK_POSITION(I2C_NACKPosition));
  
  /* Check the input parameter */
  if (I2C_NACKPosition == I2C_NACKPosition_Next)
  {
    /* Next byte in shift register is the last received byte */
    I2Cx->CR1 |= I2C_NACKPosition_Next;
  }
  else
  {
    /* Current byte in shift register is the last received byte */
    I2Cx->CR1 &= I2C_NACKPosition_Current;
  }
}

/**
  * @brief  Drives the SMBusAlert pin high or low for the specified I2C.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_SMBusAlert: specifies SMBAlert pin level. 
  *          This parameter can be one of the following values:
  *            @arg I2C_SMBusAlert_Low: SMBAlert pin driven low
  *            @arg I2C_SMBusAlert_High: SMBAlert pin driven high
  * @retval None
  */
void I2C_SMBusAlertConfig(I2C_TypeDef* I2Cx, uint16_t I2C_SMBusAlert)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_SMBUS_ALERT(I2C_SMBusAlert));
  if (I2C_SMBusAlert == I2C_SMBusAlert_Low)
  {
    /* Drive the SMBusAlert pin Low */
    I2Cx->CR1 |= I2C_SMBusAlert_Low;
  }
  else
  {
    /* Drive the SMBusAlert pin High  */
    I2Cx->CR1 &= I2C_SMBusAlert_High;
  }
}

/**
  * @brief  Enables or disables the specified I2C ARP.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2Cx ARP. 
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_ARPCmd(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected I2C ARP */
    I2Cx->CR1 |= I2C_CR1_ENARP;
  }
  else
  {
    /* Disable the selected I2C ARP */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_ENARP);
  }
}
/**
  * @}
  */

/** @defgroup I2C_Group2 Data transfers functions
 *  @brief   Data transfers functions 
 *
@verbatim   
 ===============================================================================
                        Data transfers functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Sends a data byte through the I2Cx peripheral.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  Data: Byte to be transmitted..
  * @retval None
  */
void I2C_SendData(I2C_TypeDef* I2Cx, uint8_t Data)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  /* Write in the DR register the data to be sent */
  I2Cx->DR = Data;
}

/**
  * @brief  Returns the most recent received data by the I2Cx peripheral.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @retval The value of the received data.
  */
uint8_t I2C_ReceiveData(I2C_TypeDef* I2Cx)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  /* Return the data in the DR register */
  return (uint8_t)I2Cx->DR;
}

/**
  * @}
  */

/** @defgroup I2C_Group3 PEC management functions
 *  @brief   PEC management functions 
 *
@verbatim   
 ===============================================================================
                         PEC management functions
 ===============================================================================  

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the specified I2C PEC transfer.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C PEC transmission.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_TransmitPEC(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected I2C PEC transmission */
    I2Cx->CR1 |= I2C_CR1_PEC;
  }
  else
  {
    /* Disable the selected I2C PEC transmission */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_PEC);
  }
}

/**
  * @brief  Selects the specified I2C PEC position.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_PECPosition: specifies the PEC position. 
  *          This parameter can be one of the following values:
  *            @arg I2C_PECPosition_Next: indicates that the next byte is PEC
  *            @arg I2C_PECPosition_Current: indicates that current byte is PEC
  *       
  * @note    This function configures the same bit (POS) as I2C_NACKPositionConfig()
  *          but is intended to be used in SMBUS mode while I2C_NACKPositionConfig() 
  *          is intended to used in I2C mode.
  *                
  * @retval None
  */
void I2C_PECPositionConfig(I2C_TypeDef* I2Cx, uint16_t I2C_PECPosition)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_PEC_POSITION(I2C_PECPosition));
  if (I2C_PECPosition == I2C_PECPosition_Next)
  {
    /* Next byte in shift register is PEC */
    I2Cx->CR1 |= I2C_PECPosition_Next;
  }
  else
  {
    /* Current byte in shift register is PEC */
    I2Cx->CR1 &= I2C_PECPosition_Current;
  }
}

/**
  * @brief  Enables or disables the PEC value calculation of the transferred bytes.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2Cx PEC value calculation.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_CalculatePEC(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected I2C PEC calculation */
    I2Cx->CR1 |= I2C_CR1_ENPEC;
  }
  else
  {
    /* Disable the selected I2C PEC calculation */
    I2Cx->CR1 &= (uint16_t)~((uint16_t)I2C_CR1_ENPEC);
  }
}

/**
  * @brief  Returns the PEC value for the specified I2C.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @retval The PEC value.
  */
uint8_t I2C_GetPEC(I2C_TypeDef* I2Cx)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  /* Return the selected I2C PEC value */
  return ((I2Cx->SR2) >> 8);
}

/**
  * @}
  */

/** @defgroup I2C_Group4 DMA transfers management functions
 *  @brief   DMA transfers management functions 
 *
@verbatim   
 ===============================================================================
                         DMA transfers management functions
 ===============================================================================  
  This section provides functions allowing to configure the I2C DMA channels 
  requests.
  
@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the specified I2C DMA requests.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C DMA transfer.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_DMACmd(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Enable the selected I2C DMA requests */
    I2Cx->CR2 |= I2C_CR2_DMAEN;
  }
  else
  {
    /* Disable the selected I2C DMA requests */
    I2Cx->CR2 &= (uint16_t)~((uint16_t)I2C_CR2_DMAEN);
  }
}

/**
  * @brief  Specifies that the next DMA transfer is the last one.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  NewState: new state of the I2C DMA last transfer.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_DMALastTransferCmd(I2C_TypeDef* I2Cx, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  if (NewState != DISABLE)
  {
    /* Next DMA transfer is the last transfer */
    I2Cx->CR2 |= I2C_CR2_LAST;
  }
  else
  {
    /* Next DMA transfer is not the last transfer */
    I2Cx->CR2 &= (uint16_t)~((uint16_t)I2C_CR2_LAST);
  }
}

/**
  * @}
  */

/** @defgroup I2C_Group5 Interrupts events and flags management functions
 *  @brief   Interrupts, events and flags management functions
 *
@verbatim   
 ===============================================================================
                Interrupts, events and flags management functions
 ===============================================================================  
  This section provides functions allowing to configure the I2C Interrupts 
  sources and check or clear the flags or pending bits status.
  The user should identify which mode will be used in his application to manage 
  the communication: Polling mode, Interrupt mode or DMA mode. 

 ===============================================================================
                          I2C State Monitoring Functions                    
 ===============================================================================   
  This I2C driver provides three different ways for I2C state monitoring
  depending on the application requirements and constraints:
         
   
     1. Basic state monitoring (Using I2C_CheckEvent() function)
     -----------------------------------------------------------
        It compares the status registers (SR1 and SR2) content to a given event
        (can be the combination of one or more flags).
        It returns SUCCESS if the current status includes the given flags 
        and returns ERROR if one or more flags are missing in the current status.

          - When to use
             - This function is suitable for most applications as well as for startup 
               activity since the events are fully described in the product reference 
               manual (RM0090).
             - It is also suitable for users who need to define their own events.

          - Limitations
             - If an error occurs (ie. error flags are set besides to the monitored 
               flags), the I2C_CheckEvent() function may return SUCCESS despite 
               the communication hold or corrupted real state. 
               In this case, it is advised to use error interrupts to monitor 
               the error events and handle them in the interrupt IRQ handler.
         
     @note 
         For error management, it is advised to use the following functions:
           - I2C_ITConfig() to configure and enable the error interrupts (I2C_IT_ERR).
           - I2Cx_ER_IRQHandler() which is called when the error interrupt occurs.
             Where x is the peripheral instance (I2C1, I2C2 ...)
           - I2C_GetFlagStatus() or I2C_GetITStatus()  to be called into the 
             I2Cx_ER_IRQHandler() function in order to determine which error occurred.
           - I2C_ClearFlag() or I2C_ClearITPendingBit() and/or I2C_SoftwareResetCmd() 
             and/or I2C_GenerateStop() in order to clear the error flag and source 
             and return to correct  communication status.
             
 
     2. Advanced state monitoring (Using the function I2C_GetLastEvent())
     -------------------------------------------------------------------- 
        Using the function I2C_GetLastEvent() which returns the image of both status 
        registers in a single word (uint32_t) (Status Register 2 value is shifted left 
        by 16 bits and concatenated to Status Register 1).

          - When to use
             - This function is suitable for the same applications above but it 
               allows to overcome the mentioned limitation of I2C_GetFlagStatus() 
               function.
             - The returned value could be compared to events already defined in 
               the library (stm32f4xx_i2c.h) or to custom values defined by user.
               This function is suitable when multiple flags are monitored at the 
               same time.
             - At the opposite of I2C_CheckEvent() function, this function allows 
               user to choose when an event is accepted (when all events flags are 
               set and no other flags are set or just when the needed flags are set 
               like I2C_CheckEvent() function.

          - Limitations
             - User may need to define his own events.
             - Same remark concerning the error management is applicable for this 
               function if user decides to check only regular communication flags 
               (and ignores error flags).
      
 
     3. Flag-based state monitoring (Using the function I2C_GetFlagStatus())
     -----------------------------------------------------------------------
     
      Using the function I2C_GetFlagStatus() which simply returns the status of 
      one single flag (ie. I2C_FLAG_RXNE ...). 

          - When to use
             - This function could be used for specific applications or in debug 
               phase.
             - It is suitable when only one flag checking is needed (most I2C 
               events are monitored through multiple flags).
          - Limitations: 
             - When calling this function, the Status register is accessed. 
               Some flags are cleared when the status register is accessed. 
               So checking the status of one Flag, may clear other ones.
             - Function may need to be called twice or more in order to monitor 
               one single event.
 
   For detailed description of Events, please refer to section I2C_Events in 
   stm32f4xx_i2c.h file.
       
@endverbatim
  * @{
  */
   
/**
  * @brief  Reads the specified I2C register and returns its value.
  * @param  I2C_Register: specifies the register to read.
  *          This parameter can be one of the following values:
  *            @arg I2C_Register_CR1:  CR1 register.
  *            @arg I2C_Register_CR2:   CR2 register.
  *            @arg I2C_Register_OAR1:  OAR1 register.
  *            @arg I2C_Register_OAR2:  OAR2 register.
  *            @arg I2C_Register_DR:    DR register.
  *            @arg I2C_Register_SR1:   SR1 register.
  *            @arg I2C_Register_SR2:   SR2 register.
  *            @arg I2C_Register_CCR:   CCR register.
  *            @arg I2C_Register_TRISE: TRISE register.
  * @retval The value of the read register.
  */
uint16_t I2C_ReadRegister(I2C_TypeDef* I2Cx, uint8_t I2C_Register)
{
  __IO uint32_t tmp = 0;

  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_REGISTER(I2C_Register));

  tmp = (uint32_t) I2Cx;
  tmp += I2C_Register;

  /* Return the selected register value */
  return (*(__IO uint16_t *) tmp);
}

/**
  * @brief  Enables or disables the specified I2C interrupts.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_IT: specifies the I2C interrupts sources to be enabled or disabled. 
  *          This parameter can be any combination of the following values:
  *            @arg I2C_IT_BUF: Buffer interrupt mask
  *            @arg I2C_IT_EVT: Event interrupt mask
  *            @arg I2C_IT_ERR: Error interrupt mask
  * @param  NewState: new state of the specified I2C interrupts.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void I2C_ITConfig(I2C_TypeDef* I2Cx, uint16_t I2C_IT, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  assert_param(IS_I2C_CONFIG_IT(I2C_IT));
  
  if (NewState != DISABLE)
  {
    /* Enable the selected I2C interrupts */
    I2Cx->CR2 |= I2C_IT;
  }
  else
  {
    /* Disable the selected I2C interrupts */
    I2Cx->CR2 &= (uint16_t)~I2C_IT;
  }
}

/*
 ===============================================================================
                          1. Basic state monitoring                    
 ===============================================================================  
 */

/**
  * @brief  Checks whether the last I2Cx Event is equal to the one passed
  *         as parameter.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_EVENT: specifies the event to be checked. 
  *          This parameter can be one of the following values:
  *            @arg I2C_EVENT_SLAVE_TRANSMITTER_ADDRESS_MATCHED: EV1
  *            @arg I2C_EVENT_SLAVE_RECEIVER_ADDRESS_MATCHED: EV1
  *            @arg I2C_EVENT_SLAVE_TRANSMITTER_SECONDADDRESS_MATCHED: EV1
  *            @arg I2C_EVENT_SLAVE_RECEIVER_SECONDADDRESS_MATCHED: EV1
  *            @arg I2C_EVENT_SLAVE_GENERALCALLADDRESS_MATCHED: EV1
  *            @arg I2C_EVENT_SLAVE_BYTE_RECEIVED: EV2
  *            @arg (I2C_EVENT_SLAVE_BYTE_RECEIVED | I2C_FLAG_DUALF): EV2
  *            @arg (I2C_EVENT_SLAVE_BYTE_RECEIVED | I2C_FLAG_GENCALL): EV2
  *            @arg I2C_EVENT_SLAVE_BYTE_TRANSMITTED: EV3
  *            @arg (I2C_EVENT_SLAVE_BYTE_TRANSMITTED | I2C_FLAG_DUALF): EV3
  *            @arg (I2C_EVENT_SLAVE_BYTE_TRANSMITTED | I2C_FLAG_GENCALL): EV3
  *            @arg I2C_EVENT_SLAVE_ACK_FAILURE: EV3_2
  *            @arg I2C_EVENT_SLAVE_STOP_DETECTED: EV4
  *            @arg I2C_EVENT_MASTER_MODE_SELECT: EV5
  *            @arg I2C_EVENT_MASTER_TRANSMITTER_MODE_SELECTED: EV6     
  *            @arg I2C_EVENT_MASTER_RECEIVER_MODE_SELECTED: EV6
  *            @arg I2C_EVENT_MASTER_BYTE_RECEIVED: EV7
  *            @arg I2C_EVENT_MASTER_BYTE_TRANSMITTING: EV8
  *            @arg I2C_EVENT_MASTER_BYTE_TRANSMITTED: EV8_2
  *            @arg I2C_EVENT_MASTER_MODE_ADDRESS10: EV9
  *     
  * @note   For detailed description of Events, please refer to section I2C_Events
  *         in stm32f4xx_i2c.h file.
  *    
  * @retval An ErrorStatus enumeration value:
  *           - SUCCESS: Last event is equal to the I2C_EVENT
  *           - ERROR: Last event is different from the I2C_EVENT
  */
ErrorStatus I2C_CheckEvent(I2C_TypeDef* I2Cx, uint32_t I2C_EVENT)
{
  uint32_t lastevent = 0;
  uint32_t flag1 = 0, flag2 = 0;
  ErrorStatus status = ERROR;

  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_EVENT(I2C_EVENT));

  /* Read the I2Cx status register */
  flag1 = I2Cx->SR1;
  flag2 = I2Cx->SR2;
  flag2 = flag2 << 16;

  /* Get the last event value from I2C status register */
  lastevent = (flag1 | flag2) & FLAG_MASK;

  /* Check whether the last event contains the I2C_EVENT */
  if ((lastevent & I2C_EVENT) == I2C_EVENT)
  {
    /* SUCCESS: last event is equal to I2C_EVENT */
    status = SUCCESS;
  }
  else
  {
    /* ERROR: last event is different from I2C_EVENT */
    status = ERROR;
  }
  /* Return status */
  return status;
}

/*
 ===============================================================================
                          2. Advanced state monitoring                   
 ===============================================================================  
 */

/**
  * @brief  Returns the last I2Cx Event.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  *     
  * @note   For detailed description of Events, please refer to section I2C_Events
  *         in stm32f4xx_i2c.h file.
  *    
  * @retval The last event
  */
uint32_t I2C_GetLastEvent(I2C_TypeDef* I2Cx)
{
  uint32_t lastevent = 0;
  uint32_t flag1 = 0, flag2 = 0;

  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));

  /* Read the I2Cx status register */
  flag1 = I2Cx->SR1;
  flag2 = I2Cx->SR2;
  flag2 = flag2 << 16;

  /* Get the last event value from I2C status register */
  lastevent = (flag1 | flag2) & FLAG_MASK;

  /* Return status */
  return lastevent;
}

/*
 ===============================================================================
                          3. Flag-based state monitoring                   
 ===============================================================================  
 */

/**
  * @brief  Checks whether the specified I2C flag is set or not.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_FLAG: specifies the flag to check. 
  *          This parameter can be one of the following values:
  *            @arg I2C_FLAG_DUALF: Dual flag (Slave mode)
  *            @arg I2C_FLAG_SMBHOST: SMBus host header (Slave mode)
  *            @arg I2C_FLAG_SMBDEFAULT: SMBus default header (Slave mode)
  *            @arg I2C_FLAG_GENCALL: General call header flag (Slave mode)
  *            @arg I2C_FLAG_TRA: Transmitter/Receiver flag
  *            @arg I2C_FLAG_BUSY: Bus busy flag
  *            @arg I2C_FLAG_MSL: Master/Slave flag
  *            @arg I2C_FLAG_SMBALERT: SMBus Alert flag
  *            @arg I2C_FLAG_TIMEOUT: Timeout or Tlow error flag
  *            @arg I2C_FLAG_PECERR: PEC error in reception flag
  *            @arg I2C_FLAG_OVR: Overrun/Underrun flag (Slave mode)
  *            @arg I2C_FLAG_AF: Acknowledge failure flag
  *            @arg I2C_FLAG_ARLO: Arbitration lost flag (Master mode)
  *            @arg I2C_FLAG_BERR: Bus error flag
  *            @arg I2C_FLAG_TXE: Data register empty flag (Transmitter)
  *            @arg I2C_FLAG_RXNE: Data register not empty (Receiver) flag
  *            @arg I2C_FLAG_STOPF: Stop detection flag (Slave mode)
  *            @arg I2C_FLAG_ADD10: 10-bit header sent flag (Master mode)
  *            @arg I2C_FLAG_BTF: Byte transfer finished flag
  *            @arg I2C_FLAG_ADDR: Address sent flag (Master mode) "ADSL"
  *                                Address matched flag (Slave mode)"ENDAD"
  *            @arg I2C_FLAG_SB: Start bit flag (Master mode)
  * @retval The new state of I2C_FLAG (SET or RESET).
  */
FlagStatus I2C_GetFlagStatus(I2C_TypeDef* I2Cx, uint32_t I2C_FLAG)
{
  FlagStatus bitstatus = RESET;
  __IO uint32_t i2creg = 0, i2cxbase = 0;

  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_GET_FLAG(I2C_FLAG));

  /* Get the I2Cx peripheral base address */
  i2cxbase = (uint32_t)I2Cx;
  
  /* Read flag register index */
  i2creg = I2C_FLAG >> 28;
  
  /* Get bit[23:0] of the flag */
  I2C_FLAG &= FLAG_MASK;
  
  if(i2creg != 0)
  {
    /* Get the I2Cx SR1 register address */
    i2cxbase += 0x14;
  }
  else
  {
    /* Flag in I2Cx SR2 Register */
    I2C_FLAG = (uint32_t)(I2C_FLAG >> 16);
    /* Get the I2Cx SR2 register address */
    i2cxbase += 0x18;
  }
  
  if(((*(__IO uint32_t *)i2cxbase) & I2C_FLAG) != (uint32_t)RESET)
  {
    /* I2C_FLAG is set */
    bitstatus = SET;
  }
  else
  {
    /* I2C_FLAG is reset */
    bitstatus = RESET;
  }
  
  /* Return the I2C_FLAG status */
  return  bitstatus;
}

/**
  * @brief  Clears the I2Cx's pending flags.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_FLAG: specifies the flag to clear. 
  *          This parameter can be any combination of the following values:
  *            @arg I2C_FLAG_SMBALERT: SMBus Alert flag
  *            @arg I2C_FLAG_TIMEOUT: Timeout or Tlow error flag
  *            @arg I2C_FLAG_PECERR: PEC error in reception flag
  *            @arg I2C_FLAG_OVR: Overrun/Underrun flag (Slave mode)
  *            @arg I2C_FLAG_AF: Acknowledge failure flag
  *            @arg I2C_FLAG_ARLO: Arbitration lost flag (Master mode)
  *            @arg I2C_FLAG_BERR: Bus error flag
  *   
  * @note   STOPF (STOP detection) is cleared by software sequence: a read operation 
  *          to I2C_SR1 register (I2C_GetFlagStatus()) followed by a write operation 
  *          to I2C_CR1 register (I2C_Cmd() to re-enable the I2C peripheral).
  * @note   ADD10 (10-bit header sent) is cleared by software sequence: a read 
  *          operation to I2C_SR1 (I2C_GetFlagStatus()) followed by writing the 
  *          second byte of the address in DR register.
  * @note   BTF (Byte Transfer Finished) is cleared by software sequence: a read 
  *          operation to I2C_SR1 register (I2C_GetFlagStatus()) followed by a 
  *          read/write to I2C_DR register (I2C_SendData()).
  * @note   ADDR (Address sent) is cleared by software sequence: a read operation to 
  *          I2C_SR1 register (I2C_GetFlagStatus()) followed by a read operation to 
  *          I2C_SR2 register ((void)(I2Cx->SR2)).
  * @note   SB (Start Bit) is cleared software sequence: a read operation to I2C_SR1
  *          register (I2C_GetFlagStatus()) followed by a write operation to I2C_DR
  *          register (I2C_SendData()).
  *  
  * @retval None
  */
void I2C_ClearFlag(I2C_TypeDef* I2Cx, uint32_t I2C_FLAG)
{
  uint32_t flagpos = 0;
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_CLEAR_FLAG(I2C_FLAG));
  /* Get the I2C flag position */
  flagpos = I2C_FLAG & FLAG_MASK;
  /* Clear the selected I2C flag */
  I2Cx->SR1 = (uint16_t)~flagpos;
}

/**
  * @brief  Checks whether the specified I2C interrupt has occurred or not.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_IT: specifies the interrupt source to check. 
  *          This parameter can be one of the following values:
  *            @arg I2C_IT_SMBALERT: SMBus Alert flag
  *            @arg I2C_IT_TIMEOUT: Timeout or Tlow error flag
  *            @arg I2C_IT_PECERR: PEC error in reception flag
  *            @arg I2C_IT_OVR: Overrun/Underrun flag (Slave mode)
  *            @arg I2C_IT_AF: Acknowledge failure flag
  *            @arg I2C_IT_ARLO: Arbitration lost flag (Master mode)
  *            @arg I2C_IT_BERR: Bus error flag
  *            @arg I2C_IT_TXE: Data register empty flag (Transmitter)
  *            @arg I2C_IT_RXNE: Data register not empty (Receiver) flag
  *            @arg I2C_IT_STOPF: Stop detection flag (Slave mode)
  *            @arg I2C_IT_ADD10: 10-bit header sent flag (Master mode)
  *            @arg I2C_IT_BTF: Byte transfer finished flag
  *            @arg I2C_IT_ADDR: Address sent flag (Master mode) "ADSL"
  *                              Address matched flag (Slave mode)"ENDAD"
  *            @arg I2C_IT_SB: Start bit flag (Master mode)
  * @retval The new state of I2C_IT (SET or RESET).
  */
ITStatus I2C_GetITStatus(I2C_TypeDef* I2Cx, uint32_t I2C_IT)
{
  ITStatus bitstatus = RESET;
  uint32_t enablestatus = 0;

  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_GET_IT(I2C_IT));

  /* Check if the interrupt source is enabled or not */
  enablestatus = (uint32_t)(((I2C_IT & ITEN_MASK) >> 16) & (I2Cx->CR2)) ;
  
  /* Get bit[23:0] of the flag */
  I2C_IT &= FLAG_MASK;

  /* Check the status of the specified I2C flag */
  if (((I2Cx->SR1 & I2C_IT) != (uint32_t)RESET) && enablestatus)
  {
    /* I2C_IT is set */
    bitstatus = SET;
  }
  else
  {
    /* I2C_IT is reset */
    bitstatus = RESET;
  }
  /* Return the I2C_IT status */
  return  bitstatus;
}

/**
  * @brief  Clears the I2Cx's interrupt pending bits.
  * @param  I2Cx: where x can be 1, 2 or 3 to select the I2C peripheral.
  * @param  I2C_IT: specifies the interrupt pending bit to clear. 
  *          This parameter can be any combination of the following values:
  *            @arg I2C_IT_SMBALERT: SMBus Alert interrupt
  *            @arg I2C_IT_TIMEOUT: Timeout or Tlow error interrupt
  *            @arg I2C_IT_PECERR: PEC error in reception  interrupt
  *            @arg I2C_IT_OVR: Overrun/Underrun interrupt (Slave mode)
  *            @arg I2C_IT_AF: Acknowledge failure interrupt
  *            @arg I2C_IT_ARLO: Arbitration lost interrupt (Master mode)
  *            @arg I2C_IT_BERR: Bus error interrupt
  * 
  * @note   STOPF (STOP detection) is cleared by software sequence: a read operation 
  *          to I2C_SR1 register (I2C_GetITStatus()) followed by a write operation to 
  *          I2C_CR1 register (I2C_Cmd() to re-enable the I2C peripheral).
  * @note   ADD10 (10-bit header sent) is cleared by software sequence: a read 
  *          operation to I2C_SR1 (I2C_GetITStatus()) followed by writing the second 
  *          byte of the address in I2C_DR register.
  * @note   BTF (Byte Transfer Finished) is cleared by software sequence: a read 
  *          operation to I2C_SR1 register (I2C_GetITStatus()) followed by a 
  *          read/write to I2C_DR register (I2C_SendData()).
  * @note   ADDR (Address sent) is cleared by software sequence: a read operation to 
  *          I2C_SR1 register (I2C_GetITStatus()) followed by a read operation to 
  *          I2C_SR2 register ((void)(I2Cx->SR2)).
  * @note   SB (Start Bit) is cleared by software sequence: a read operation to 
  *          I2C_SR1 register (I2C_GetITStatus()) followed by a write operation to 
  *          I2C_DR register (I2C_SendData()).
  * @retval None
  */
void I2C_ClearITPendingBit(I2C_TypeDef* I2Cx, uint32_t I2C_IT)
{
  uint32_t flagpos = 0;
  /* Check the parameters */
  assert_param(IS_I2C_ALL_PERIPH(I2Cx));
  assert_param(IS_I2C_CLEAR_IT(I2C_IT));

  /* Get the I2C flag position */
  flagpos = I2C_IT & FLAG_MASK;

  /* Clear the selected I2C flag */
  I2Cx->SR1 = (uint16_t)~flagpos;
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
