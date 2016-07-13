/**
  ******************************************************************************
  * @file    stm32f4xx_sdio.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides firmware functions to manage the following 
  *          functionalities of the Secure digital input/output interface (SDIO) 
  *          peripheral:
  *           - Initialization and Configuration
  *           - Command path state machine (CPSM) management
  *           - Data path state machine (DPSM) management
  *           - SDIO IO Cards mode management
  *           - CE-ATA mode management
  *           - DMA transfers management
  *           - Interrupts and flags management
  *
  *  @verbatim
  *
  *
  *          ===================================================================
  *                                 How to use this driver
  *          ===================================================================
  *          1. The SDIO clock (SDIOCLK = 48 MHz) is coming from a specific output
  *             of PLL (PLL48CLK). Before to start working with SDIO peripheral
  *             make sure that the PLL is well configured.
  *          The SDIO peripheral uses two clock signals:
  *              - SDIO adapter clock (SDIOCLK = 48 MHz)
  *              - APB2 bus clock (PCLK2)
  *          PCLK2 and SDIO_CK clock frequencies must respect the following condition:
  *                   Frequenc(PCLK2) >= (3 / 8 x Frequency(SDIO_CK))
  *
  *          2. Enable peripheral clock using RCC_APB2PeriphClockCmd(RCC_APB2Periph_SDIO, ENABLE).
  *
  *          3.  According to the SDIO mode, enable the GPIO clocks using 
  *              RCC_AHB1PeriphClockCmd() function. 
  *              The I/O can be one of the following configurations:
  *                 - 1-bit data length: SDIO_CMD, SDIO_CK and D0.
  *                 - 4-bit data length: SDIO_CMD, SDIO_CK and D[3:0].
  *                 - 8-bit data length: SDIO_CMD, SDIO_CK and D[7:0].      
  *
  *          4. Peripheral's alternate function: 
  *                 - Connect the pin to the desired peripherals' Alternate 
  *                   Function (AF) using GPIO_PinAFConfig() function
  *                 - Configure the desired pin in alternate function by:
  *                   GPIO_InitStruct->GPIO_Mode = GPIO_Mode_AF
  *                 - Select the type, pull-up/pull-down and output speed via 
  *                   GPIO_PuPd, GPIO_OType and GPIO_Speed members
  *                 - Call GPIO_Init() function
  *
  *          5. Program the Clock Edge, Clock Bypass, Clock Power Save, Bus Wide, 
  *             hardware, flow control and the Clock Divider using the SDIO_Init()
  *             function.
  *
  *          6. Enable the Power ON State using the SDIO_SetPowerState(SDIO_PowerState_ON) 
  *             function.
  *              
  *          7. Enable the clock using the SDIO_ClockCmd() function.
  *
  *          8. Enable the NVIC and the corresponding interrupt using the function 
  *             SDIO_ITConfig() if you need to use interrupt mode. 
  *
  *          9. When using the DMA mode 
  *                   - Configure the DMA using DMA_Init() function
  *                   - Active the needed channel Request using SDIO_DMACmd() function
  *
  *          10. Enable the DMA using the DMA_Cmd() function, when using DMA mode. 
  *
  *          11. To control the CPSM (Command Path State Machine) and send 
  *              commands to the card use the SDIO_SendCommand(), 
  *              SDIO_GetCommandResponse() and SDIO_GetResponse() functions.     
  *              First, user has to fill the command structure (pointer to
  *              SDIO_CmdInitTypeDef) according to the selected command to be sent.
  *                 The parameters that should be filled are:
  *                   - Command Argument
  *                   - Command Index
  *                   - Command Response type
  *                   - Command Wait
  *                   - CPSM Status (Enable or Disable)
  *
  *              To check if the command is well received, read the SDIO_CMDRESP
  *              register using the SDIO_GetCommandResponse().
  *              The SDIO responses registers (SDIO_RESP1 to SDIO_RESP2), use the
  *              SDIO_GetResponse() function.
  *
  *          12. To control the DPSM (Data Path State Machine) and send/receive 
  *              data to/from the card use the SDIO_DataConfig(), SDIO_GetDataCounter(), 
  *              SDIO_ReadData(), SDIO_WriteData() and SDIO_GetFIFOCount() functions.
  *
  *              Read Operations
  *              ---------------
  *              a) First, user has to fill the data structure (pointer to
  *                 SDIO_DataInitTypeDef) according to the selected data type to
  *                 be received.
  *                 The parameters that should be filled are:
  *                   - Data TimeOut
  *                   - Data Length
  *                   - Data Block size
  *                   - Data Transfer direction: should be from card (To SDIO)
  *                   - Data Transfer mode
  *                   - DPSM Status (Enable or Disable)
  *                                   
  *              b) Configure the SDIO resources to receive the data from the card
  *                 according to selected transfer mode (Refer to Step 8, 9 and 10).
  *
  *              c) Send the selected Read command (refer to step 11).
  *                  
  *              d) Use the SDIO flags/interrupts to check the transfer status.
  *
  *              Write Operations
  *              ---------------
  *              a) First, user has to fill the data structure (pointer to
  *                 SDIO_DataInitTypeDef) according to the selected data type to
  *                 be received.
  *                 The parameters that should be filled are:
  *                   - Data TimeOut
  *                   - Data Length
  *                   - Data Block size
  *                   - Data Transfer direction:  should be to card (To CARD)
  *                   - Data Transfer mode
  *                   - DPSM Status (Enable or Disable)
  *
  *              b) Configure the SDIO resources to send the data to the card
  *                 according to selected transfer mode (Refer to Step 8, 9 and 10).
  *                   
  *              c) Send the selected Write command (refer to step 11).
  *                  
  *              d) Use the SDIO flags/interrupts to check the transfer status.
  *
  *
  *  @endverbatim
  *
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
#include "stm32f4xx_sdio.h"
#include "stm32f4xx_rcc.h"

/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup SDIO 
  * @brief SDIO driver modules
  * @{
  */ 

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/

/* ------------ SDIO registers bit address in the alias region ----------- */
#define SDIO_OFFSET                (SDIO_BASE - PERIPH_BASE)

/* --- CLKCR Register ---*/
/* Alias word address of CLKEN bit */
#define CLKCR_OFFSET              (SDIO_OFFSET + 0x04)
#define CLKEN_BitNumber           0x08
#define CLKCR_CLKEN_BB            (PERIPH_BB_BASE + (CLKCR_OFFSET * 32) + (CLKEN_BitNumber * 4))

/* --- CMD Register ---*/
/* Alias word address of SDIOSUSPEND bit */
#define CMD_OFFSET                (SDIO_OFFSET + 0x0C)
#define SDIOSUSPEND_BitNumber     0x0B
#define CMD_SDIOSUSPEND_BB        (PERIPH_BB_BASE + (CMD_OFFSET * 32) + (SDIOSUSPEND_BitNumber * 4))

/* Alias word address of ENCMDCOMPL bit */
#define ENCMDCOMPL_BitNumber      0x0C
#define CMD_ENCMDCOMPL_BB         (PERIPH_BB_BASE + (CMD_OFFSET * 32) + (ENCMDCOMPL_BitNumber * 4))

/* Alias word address of NIEN bit */
#define NIEN_BitNumber            0x0D
#define CMD_NIEN_BB               (PERIPH_BB_BASE + (CMD_OFFSET * 32) + (NIEN_BitNumber * 4))

/* Alias word address of ATACMD bit */
#define ATACMD_BitNumber          0x0E
#define CMD_ATACMD_BB             (PERIPH_BB_BASE + (CMD_OFFSET * 32) + (ATACMD_BitNumber * 4))

/* --- DCTRL Register ---*/
/* Alias word address of DMAEN bit */
#define DCTRL_OFFSET              (SDIO_OFFSET + 0x2C)
#define DMAEN_BitNumber           0x03
#define DCTRL_DMAEN_BB            (PERIPH_BB_BASE + (DCTRL_OFFSET * 32) + (DMAEN_BitNumber * 4))

/* Alias word address of RWSTART bit */
#define RWSTART_BitNumber         0x08
#define DCTRL_RWSTART_BB          (PERIPH_BB_BASE + (DCTRL_OFFSET * 32) + (RWSTART_BitNumber * 4))

/* Alias word address of RWSTOP bit */
#define RWSTOP_BitNumber          0x09
#define DCTRL_RWSTOP_BB           (PERIPH_BB_BASE + (DCTRL_OFFSET * 32) + (RWSTOP_BitNumber * 4))

/* Alias word address of RWMOD bit */
#define RWMOD_BitNumber           0x0A
#define DCTRL_RWMOD_BB            (PERIPH_BB_BASE + (DCTRL_OFFSET * 32) + (RWMOD_BitNumber * 4))

/* Alias word address of SDIOEN bit */
#define SDIOEN_BitNumber          0x0B
#define DCTRL_SDIOEN_BB           (PERIPH_BB_BASE + (DCTRL_OFFSET * 32) + (SDIOEN_BitNumber * 4))

/* ---------------------- SDIO registers bit mask ------------------------ */
/* --- CLKCR Register ---*/
/* CLKCR register clear mask */
#define CLKCR_CLEAR_MASK         ((uint32_t)0xFFFF8100) 

/* --- PWRCTRL Register ---*/
/* SDIO PWRCTRL Mask */
#define PWR_PWRCTRL_MASK         ((uint32_t)0xFFFFFFFC)

/* --- DCTRL Register ---*/
/* SDIO DCTRL Clear Mask */
#define DCTRL_CLEAR_MASK         ((uint32_t)0xFFFFFF08)

/* --- CMD Register ---*/
/* CMD Register clear mask */
#define CMD_CLEAR_MASK           ((uint32_t)0xFFFFF800)

/* SDIO RESP Registers Address */
#define SDIO_RESP_ADDR           ((uint32_t)(SDIO_BASE + 0x14))

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/

/** @defgroup SDIO_Private_Functions
  * @{
  */

/** @defgroup SDIO_Group1 Initialization and Configuration functions
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
  * @brief  Deinitializes the SDIO peripheral registers to their default reset values.
  * @param  None
  * @retval None
  */
void SDIO_DeInit(void)
{
  RCC_APB2PeriphResetCmd(RCC_APB2Periph_SDIO, ENABLE);
  RCC_APB2PeriphResetCmd(RCC_APB2Periph_SDIO, DISABLE);
}

/**
  * @brief  Initializes the SDIO peripheral according to the specified 
  *         parameters in the SDIO_InitStruct.
  * @param  SDIO_InitStruct : pointer to a SDIO_InitTypeDef structure 
  *         that contains the configuration information for the SDIO peripheral.
  * @retval None
  */
void SDIO_Init(SDIO_InitTypeDef* SDIO_InitStruct)
{
  uint32_t tmpreg = 0;
    
  /* Check the parameters */
  assert_param(IS_SDIO_CLOCK_EDGE(SDIO_InitStruct->SDIO_ClockEdge));
  assert_param(IS_SDIO_CLOCK_BYPASS(SDIO_InitStruct->SDIO_ClockBypass));
  assert_param(IS_SDIO_CLOCK_POWER_SAVE(SDIO_InitStruct->SDIO_ClockPowerSave));
  assert_param(IS_SDIO_BUS_WIDE(SDIO_InitStruct->SDIO_BusWide));
  assert_param(IS_SDIO_HARDWARE_FLOW_CONTROL(SDIO_InitStruct->SDIO_HardwareFlowControl)); 
   
/*---------------------------- SDIO CLKCR Configuration ------------------------*/  
  /* Get the SDIO CLKCR value */
  tmpreg = SDIO->CLKCR;
  
  /* Clear CLKDIV, PWRSAV, BYPASS, WIDBUS, NEGEDGE, HWFC_EN bits */
  tmpreg &= CLKCR_CLEAR_MASK;
  
  /* Set CLKDIV bits according to SDIO_ClockDiv value */
  /* Set PWRSAV bit according to SDIO_ClockPowerSave value */
  /* Set BYPASS bit according to SDIO_ClockBypass value */
  /* Set WIDBUS bits according to SDIO_BusWide value */
  /* Set NEGEDGE bits according to SDIO_ClockEdge value */
  /* Set HWFC_EN bits according to SDIO_HardwareFlowControl value */
  tmpreg |= (SDIO_InitStruct->SDIO_ClockDiv  | SDIO_InitStruct->SDIO_ClockPowerSave |
             SDIO_InitStruct->SDIO_ClockBypass | SDIO_InitStruct->SDIO_BusWide |
             SDIO_InitStruct->SDIO_ClockEdge | SDIO_InitStruct->SDIO_HardwareFlowControl); 
  
  /* Write to SDIO CLKCR */
  SDIO->CLKCR = tmpreg;
}

/**
  * @brief  Fills each SDIO_InitStruct member with its default value.
  * @param  SDIO_InitStruct: pointer to an SDIO_InitTypeDef structure which 
  *         will be initialized.
  * @retval None
  */
void SDIO_StructInit(SDIO_InitTypeDef* SDIO_InitStruct)
{
  /* SDIO_InitStruct members default value */
  SDIO_InitStruct->SDIO_ClockDiv = 0x00;
  SDIO_InitStruct->SDIO_ClockEdge = SDIO_ClockEdge_Rising;
  SDIO_InitStruct->SDIO_ClockBypass = SDIO_ClockBypass_Disable;
  SDIO_InitStruct->SDIO_ClockPowerSave = SDIO_ClockPowerSave_Disable;
  SDIO_InitStruct->SDIO_BusWide = SDIO_BusWide_1b;
  SDIO_InitStruct->SDIO_HardwareFlowControl = SDIO_HardwareFlowControl_Disable;
}

/**
  * @brief  Enables or disables the SDIO Clock.
  * @param  NewState: new state of the SDIO Clock. 
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_ClockCmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) CLKCR_CLKEN_BB = (uint32_t)NewState;
}

/**
  * @brief  Sets the power status of the controller.
  * @param  SDIO_PowerState: new state of the Power state. 
  *          This parameter can be one of the following values:
  *            @arg SDIO_PowerState_OFF: SDIO Power OFF
  *            @arg SDIO_PowerState_ON: SDIO Power ON
  * @retval None
  */
void SDIO_SetPowerState(uint32_t SDIO_PowerState)
{
  /* Check the parameters */
  assert_param(IS_SDIO_POWER_STATE(SDIO_PowerState));
  
  SDIO->POWER = SDIO_PowerState;
}

/**
  * @brief  Gets the power status of the controller.
  * @param  None
  * @retval Power status of the controller. The returned value can be one of the 
  *         following values:
  *            - 0x00: Power OFF
  *            - 0x02: Power UP
  *            - 0x03: Power ON 
  */
uint32_t SDIO_GetPowerState(void)
{
  return (SDIO->POWER & (~PWR_PWRCTRL_MASK));
}

/**
  * @}
  */

/** @defgroup SDIO_Group2 Command path state machine (CPSM) management functions
 *  @brief   Command path state machine (CPSM) management functions 
 *
@verbatim   
 ===============================================================================
              Command path state machine (CPSM) management functions
 ===============================================================================  

  This section provide functions allowing to program and read the Command path 
  state machine (CPSM).

@endverbatim
  * @{
  */

/**
  * @brief  Initializes the SDIO Command according to the specified 
  *         parameters in the SDIO_CmdInitStruct and send the command.
  * @param  SDIO_CmdInitStruct : pointer to a SDIO_CmdInitTypeDef 
  *         structure that contains the configuration information for the SDIO 
  *         command.
  * @retval None
  */
void SDIO_SendCommand(SDIO_CmdInitTypeDef *SDIO_CmdInitStruct)
{
  uint32_t tmpreg = 0;
  
  /* Check the parameters */
  assert_param(IS_SDIO_CMD_INDEX(SDIO_CmdInitStruct->SDIO_CmdIndex));
  assert_param(IS_SDIO_RESPONSE(SDIO_CmdInitStruct->SDIO_Response));
  assert_param(IS_SDIO_WAIT(SDIO_CmdInitStruct->SDIO_Wait));
  assert_param(IS_SDIO_CPSM(SDIO_CmdInitStruct->SDIO_CPSM));
  
/*---------------------------- SDIO ARG Configuration ------------------------*/
  /* Set the SDIO Argument value */
  SDIO->ARG = SDIO_CmdInitStruct->SDIO_Argument;
  
/*---------------------------- SDIO CMD Configuration ------------------------*/  
  /* Get the SDIO CMD value */
  tmpreg = SDIO->CMD;
  /* Clear CMDINDEX, WAITRESP, WAITINT, WAITPEND, CPSMEN bits */
  tmpreg &= CMD_CLEAR_MASK;
  /* Set CMDINDEX bits according to SDIO_CmdIndex value */
  /* Set WAITRESP bits according to SDIO_Response value */
  /* Set WAITINT and WAITPEND bits according to SDIO_Wait value */
  /* Set CPSMEN bits according to SDIO_CPSM value */
  tmpreg |= (uint32_t)SDIO_CmdInitStruct->SDIO_CmdIndex | SDIO_CmdInitStruct->SDIO_Response
           | SDIO_CmdInitStruct->SDIO_Wait | SDIO_CmdInitStruct->SDIO_CPSM;
  
  /* Write to SDIO CMD */
  SDIO->CMD = tmpreg;
}

/**
  * @brief  Fills each SDIO_CmdInitStruct member with its default value.
  * @param  SDIO_CmdInitStruct: pointer to an SDIO_CmdInitTypeDef 
  *         structure which will be initialized.
  * @retval None
  */
void SDIO_CmdStructInit(SDIO_CmdInitTypeDef* SDIO_CmdInitStruct)
{
  /* SDIO_CmdInitStruct members default value */
  SDIO_CmdInitStruct->SDIO_Argument = 0x00;
  SDIO_CmdInitStruct->SDIO_CmdIndex = 0x00;
  SDIO_CmdInitStruct->SDIO_Response = SDIO_Response_No;
  SDIO_CmdInitStruct->SDIO_Wait = SDIO_Wait_No;
  SDIO_CmdInitStruct->SDIO_CPSM = SDIO_CPSM_Disable;
}

/**
  * @brief  Returns command index of last command for which response received.
  * @param  None
  * @retval Returns the command index of the last command response received.
  */
uint8_t SDIO_GetCommandResponse(void)
{
  return (uint8_t)(SDIO->RESPCMD);
}

/**
  * @brief  Returns response received from the card for the last command.
  * @param  SDIO_RESP: Specifies the SDIO response register. 
  *          This parameter can be one of the following values:
  *            @arg SDIO_RESP1: Response Register 1
  *            @arg SDIO_RESP2: Response Register 2
  *            @arg SDIO_RESP3: Response Register 3
  *            @arg SDIO_RESP4: Response Register 4
  * @retval The Corresponding response register value.
  */
uint32_t SDIO_GetResponse(uint32_t SDIO_RESP)
{
  __IO uint32_t tmp = 0;

  /* Check the parameters */
  assert_param(IS_SDIO_RESP(SDIO_RESP));

  tmp = SDIO_RESP_ADDR + SDIO_RESP;
  
  return (*(__IO uint32_t *) tmp); 
}

/**
  * @}
  */

/** @defgroup SDIO_Group3 Data path state machine (DPSM) management functions
 *  @brief   Data path state machine (DPSM) management functions
 *
@verbatim   
 ===============================================================================
              Data path state machine (DPSM) management functions
 ===============================================================================  

  This section provide functions allowing to program and read the Data path 
  state machine (DPSM).

@endverbatim
  * @{
  */

/**
  * @brief  Initializes the SDIO data path according to the specified 
  *         parameters in the SDIO_DataInitStruct.
  * @param  SDIO_DataInitStruct : pointer to a SDIO_DataInitTypeDef structure 
  *         that contains the configuration information for the SDIO command.
  * @retval None
  */
void SDIO_DataConfig(SDIO_DataInitTypeDef* SDIO_DataInitStruct)
{
  uint32_t tmpreg = 0;
  
  /* Check the parameters */
  assert_param(IS_SDIO_DATA_LENGTH(SDIO_DataInitStruct->SDIO_DataLength));
  assert_param(IS_SDIO_BLOCK_SIZE(SDIO_DataInitStruct->SDIO_DataBlockSize));
  assert_param(IS_SDIO_TRANSFER_DIR(SDIO_DataInitStruct->SDIO_TransferDir));
  assert_param(IS_SDIO_TRANSFER_MODE(SDIO_DataInitStruct->SDIO_TransferMode));
  assert_param(IS_SDIO_DPSM(SDIO_DataInitStruct->SDIO_DPSM));

/*---------------------------- SDIO DTIMER Configuration ---------------------*/
  /* Set the SDIO Data TimeOut value */
  SDIO->DTIMER = SDIO_DataInitStruct->SDIO_DataTimeOut;

/*---------------------------- SDIO DLEN Configuration -----------------------*/
  /* Set the SDIO DataLength value */
  SDIO->DLEN = SDIO_DataInitStruct->SDIO_DataLength;

/*---------------------------- SDIO DCTRL Configuration ----------------------*/  
  /* Get the SDIO DCTRL value */
  tmpreg = SDIO->DCTRL;
  /* Clear DEN, DTMODE, DTDIR and DBCKSIZE bits */
  tmpreg &= DCTRL_CLEAR_MASK;
  /* Set DEN bit according to SDIO_DPSM value */
  /* Set DTMODE bit according to SDIO_TransferMode value */
  /* Set DTDIR bit according to SDIO_TransferDir value */
  /* Set DBCKSIZE bits according to SDIO_DataBlockSize value */
  tmpreg |= (uint32_t)SDIO_DataInitStruct->SDIO_DataBlockSize | SDIO_DataInitStruct->SDIO_TransferDir
           | SDIO_DataInitStruct->SDIO_TransferMode | SDIO_DataInitStruct->SDIO_DPSM;

  /* Write to SDIO DCTRL */
  SDIO->DCTRL = tmpreg;
}

/**
  * @brief  Fills each SDIO_DataInitStruct member with its default value.
  * @param  SDIO_DataInitStruct: pointer to an SDIO_DataInitTypeDef structure 
  *         which will be initialized.
  * @retval None
  */
void SDIO_DataStructInit(SDIO_DataInitTypeDef* SDIO_DataInitStruct)
{
  /* SDIO_DataInitStruct members default value */
  SDIO_DataInitStruct->SDIO_DataTimeOut = 0xFFFFFFFF;
  SDIO_DataInitStruct->SDIO_DataLength = 0x00;
  SDIO_DataInitStruct->SDIO_DataBlockSize = SDIO_DataBlockSize_1b;
  SDIO_DataInitStruct->SDIO_TransferDir = SDIO_TransferDir_ToCard;
  SDIO_DataInitStruct->SDIO_TransferMode = SDIO_TransferMode_Block;  
  SDIO_DataInitStruct->SDIO_DPSM = SDIO_DPSM_Disable;
}

/**
  * @brief  Returns number of remaining data bytes to be transferred.
  * @param  None
  * @retval Number of remaining data bytes to be transferred
  */
uint32_t SDIO_GetDataCounter(void)
{ 
  return SDIO->DCOUNT;
}

/**
  * @brief  Read one data word from Rx FIFO.
  * @param  None
  * @retval Data received
  */
uint32_t SDIO_ReadData(void)
{ 
  return SDIO->FIFO;
}

/**
  * @brief  Write one data word to Tx FIFO.
  * @param  Data: 32-bit data word to write.
  * @retval None
  */
void SDIO_WriteData(uint32_t Data)
{ 
  SDIO->FIFO = Data;
}

/**
  * @brief  Returns the number of words left to be written to or read from FIFO.	
  * @param  None
  * @retval Remaining number of words.
  */
uint32_t SDIO_GetFIFOCount(void)
{ 
  return SDIO->FIFOCNT;
}

/**
  * @}
  */

/** @defgroup SDIO_Group4 SDIO IO Cards mode management functions
 *  @brief   SDIO IO Cards mode management functions
 *
@verbatim   
 ===============================================================================
              SDIO IO Cards mode management functions
 ===============================================================================  

  This section provide functions allowing to program and read the SDIO IO Cards.

@endverbatim
  * @{
  */

/**
  * @brief  Starts the SD I/O Read Wait operation.	
  * @param  NewState: new state of the Start SDIO Read Wait operation. 
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_StartSDIOReadWait(FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) DCTRL_RWSTART_BB = (uint32_t) NewState;
}

/**
  * @brief  Stops the SD I/O Read Wait operation.	
  * @param  NewState: new state of the Stop SDIO Read Wait operation. 
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_StopSDIOReadWait(FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) DCTRL_RWSTOP_BB = (uint32_t) NewState;
}

/**
  * @brief  Sets one of the two options of inserting read wait interval.
  * @param  SDIO_ReadWaitMode: SD I/O Read Wait operation mode.
  *          This parameter can be:
  *            @arg SDIO_ReadWaitMode_CLK: Read Wait control by stopping SDIOCLK
  *            @arg SDIO_ReadWaitMode_DATA2: Read Wait control using SDIO_DATA2
  * @retval None
  */
void SDIO_SetSDIOReadWaitMode(uint32_t SDIO_ReadWaitMode)
{
  /* Check the parameters */
  assert_param(IS_SDIO_READWAIT_MODE(SDIO_ReadWaitMode));
  
  *(__IO uint32_t *) DCTRL_RWMOD_BB = SDIO_ReadWaitMode;
}

/**
  * @brief  Enables or disables the SD I/O Mode Operation.
  * @param  NewState: new state of SDIO specific operation. 
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_SetSDIOOperation(FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) DCTRL_SDIOEN_BB = (uint32_t)NewState;
}

/**
  * @brief  Enables or disables the SD I/O Mode suspend command sending.
  * @param  NewState: new state of the SD I/O Mode suspend command.
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_SendSDIOSuspendCmd(FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) CMD_SDIOSUSPEND_BB = (uint32_t)NewState;
}

/**
  * @}
  */

/** @defgroup SDIO_Group5 CE-ATA mode management functions
 *  @brief   CE-ATA mode management functions
 *
@verbatim   
 ===============================================================================
              CE-ATA mode management functions
 ===============================================================================  

  This section provide functions allowing to program and read the CE-ATA card.

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the command completion signal.
  * @param  NewState: new state of command completion signal. 
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_CommandCompletionCmd(FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) CMD_ENCMDCOMPL_BB = (uint32_t)NewState;
}

/**
  * @brief  Enables or disables the CE-ATA interrupt.
  * @param  NewState: new state of CE-ATA interrupt. 
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_CEATAITCmd(FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) CMD_NIEN_BB = (uint32_t)((~((uint32_t)NewState)) & ((uint32_t)0x1));
}

/**
  * @brief  Sends CE-ATA command (CMD61).
  * @param  NewState: new state of CE-ATA command. 
  *         This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_SendCEATACmd(FunctionalState NewState)
{ 
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) CMD_ATACMD_BB = (uint32_t)NewState;
}

/**
  * @}
  */

/** @defgroup SDIO_Group6 DMA transfers management functions
 *  @brief   DMA transfers management functions
 *
@verbatim   
 ===============================================================================
              DMA transfers management functions
 ===============================================================================  

  This section provide functions allowing to program SDIO DMA transfer.

@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the SDIO DMA request.
  * @param  NewState: new state of the selected SDIO DMA request.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None
  */
void SDIO_DMACmd(FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  *(__IO uint32_t *) DCTRL_DMAEN_BB = (uint32_t)NewState;
}

/**
  * @}
  */

/** @defgroup SDIO_Group7 Interrupts and flags management functions
 *  @brief   Interrupts and flags management functions  
 *
@verbatim   
 ===============================================================================
                       Interrupts and flags management functions
 ===============================================================================  


@endverbatim
  * @{
  */

/**
  * @brief  Enables or disables the SDIO interrupts.
  * @param  SDIO_IT: specifies the SDIO interrupt sources to be enabled or disabled.
  *          This parameter can be one or a combination of the following values:
  *            @arg SDIO_IT_CCRCFAIL: Command response received (CRC check failed) interrupt
  *            @arg SDIO_IT_DCRCFAIL: Data block sent/received (CRC check failed) interrupt
  *            @arg SDIO_IT_CTIMEOUT: Command response timeout interrupt
  *            @arg SDIO_IT_DTIMEOUT: Data timeout interrupt
  *            @arg SDIO_IT_TXUNDERR: Transmit FIFO underrun error interrupt
  *            @arg SDIO_IT_RXOVERR:  Received FIFO overrun error interrupt
  *            @arg SDIO_IT_CMDREND:  Command response received (CRC check passed) interrupt
  *            @arg SDIO_IT_CMDSENT:  Command sent (no response required) interrupt
  *            @arg SDIO_IT_DATAEND:  Data end (data counter, SDIDCOUNT, is zero) interrupt
  *            @arg SDIO_IT_STBITERR: Start bit not detected on all data signals in wide 
  *                                   bus mode interrupt
  *            @arg SDIO_IT_DBCKEND:  Data block sent/received (CRC check passed) interrupt
  *            @arg SDIO_IT_CMDACT:   Command transfer in progress interrupt
  *            @arg SDIO_IT_TXACT:    Data transmit in progress interrupt
  *            @arg SDIO_IT_RXACT:    Data receive in progress interrupt
  *            @arg SDIO_IT_TXFIFOHE: Transmit FIFO Half Empty interrupt
  *            @arg SDIO_IT_RXFIFOHF: Receive FIFO Half Full interrupt
  *            @arg SDIO_IT_TXFIFOF:  Transmit FIFO full interrupt
  *            @arg SDIO_IT_RXFIFOF:  Receive FIFO full interrupt
  *            @arg SDIO_IT_TXFIFOE:  Transmit FIFO empty interrupt
  *            @arg SDIO_IT_RXFIFOE:  Receive FIFO empty interrupt
  *            @arg SDIO_IT_TXDAVL:   Data available in transmit FIFO interrupt
  *            @arg SDIO_IT_RXDAVL:   Data available in receive FIFO interrupt
  *            @arg SDIO_IT_SDIOIT:   SD I/O interrupt received interrupt
  *            @arg SDIO_IT_CEATAEND: CE-ATA command completion signal received for CMD61 interrupt
  * @param  NewState: new state of the specified SDIO interrupts.
  *          This parameter can be: ENABLE or DISABLE.
  * @retval None 
  */
void SDIO_ITConfig(uint32_t SDIO_IT, FunctionalState NewState)
{
  /* Check the parameters */
  assert_param(IS_SDIO_IT(SDIO_IT));
  assert_param(IS_FUNCTIONAL_STATE(NewState));
  
  if (NewState != DISABLE)
  {
    /* Enable the SDIO interrupts */
    SDIO->MASK |= SDIO_IT;
  }
  else
  {
    /* Disable the SDIO interrupts */
    SDIO->MASK &= ~SDIO_IT;
  } 
}

/**
  * @brief  Checks whether the specified SDIO flag is set or not.
  * @param  SDIO_FLAG: specifies the flag to check. 
  *          This parameter can be one of the following values:
  *            @arg SDIO_FLAG_CCRCFAIL: Command response received (CRC check failed)
  *            @arg SDIO_FLAG_DCRCFAIL: Data block sent/received (CRC check failed)
  *            @arg SDIO_FLAG_CTIMEOUT: Command response timeout
  *            @arg SDIO_FLAG_DTIMEOUT: Data timeout
  *            @arg SDIO_FLAG_TXUNDERR: Transmit FIFO underrun error
  *            @arg SDIO_FLAG_RXOVERR:  Received FIFO overrun error
  *            @arg SDIO_FLAG_CMDREND:  Command response received (CRC check passed)
  *            @arg SDIO_FLAG_CMDSENT:  Command sent (no response required)
  *            @arg SDIO_FLAG_DATAEND:  Data end (data counter, SDIDCOUNT, is zero)
  *            @arg SDIO_FLAG_STBITERR: Start bit not detected on all data signals in wide bus mode.
  *            @arg SDIO_FLAG_DBCKEND:  Data block sent/received (CRC check passed)
  *            @arg SDIO_FLAG_CMDACT:   Command transfer in progress
  *            @arg SDIO_FLAG_TXACT:    Data transmit in progress
  *            @arg SDIO_FLAG_RXACT:    Data receive in progress
  *            @arg SDIO_FLAG_TXFIFOHE: Transmit FIFO Half Empty
  *            @arg SDIO_FLAG_RXFIFOHF: Receive FIFO Half Full
  *            @arg SDIO_FLAG_TXFIFOF:  Transmit FIFO full
  *            @arg SDIO_FLAG_RXFIFOF:  Receive FIFO full
  *            @arg SDIO_FLAG_TXFIFOE:  Transmit FIFO empty
  *            @arg SDIO_FLAG_RXFIFOE:  Receive FIFO empty
  *            @arg SDIO_FLAG_TXDAVL:   Data available in transmit FIFO
  *            @arg SDIO_FLAG_RXDAVL:   Data available in receive FIFO
  *            @arg SDIO_FLAG_SDIOIT:   SD I/O interrupt received
  *            @arg SDIO_FLAG_CEATAEND: CE-ATA command completion signal received for CMD61
  * @retval The new state of SDIO_FLAG (SET or RESET).
  */
FlagStatus SDIO_GetFlagStatus(uint32_t SDIO_FLAG)
{ 
  FlagStatus bitstatus = RESET;
  
  /* Check the parameters */
  assert_param(IS_SDIO_FLAG(SDIO_FLAG));
  
  if ((SDIO->STA & SDIO_FLAG) != (uint32_t)RESET)
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
  * @brief  Clears the SDIO's pending flags.
  * @param  SDIO_FLAG: specifies the flag to clear.  
  *          This parameter can be one or a combination of the following values:
  *            @arg SDIO_FLAG_CCRCFAIL: Command response received (CRC check failed)
  *            @arg SDIO_FLAG_DCRCFAIL: Data block sent/received (CRC check failed)
  *            @arg SDIO_FLAG_CTIMEOUT: Command response timeout
  *            @arg SDIO_FLAG_DTIMEOUT: Data timeout
  *            @arg SDIO_FLAG_TXUNDERR: Transmit FIFO underrun error
  *            @arg SDIO_FLAG_RXOVERR:  Received FIFO overrun error
  *            @arg SDIO_FLAG_CMDREND:  Command response received (CRC check passed)
  *            @arg SDIO_FLAG_CMDSENT:  Command sent (no response required)
  *            @arg SDIO_FLAG_DATAEND:  Data end (data counter, SDIDCOUNT, is zero)
  *            @arg SDIO_FLAG_STBITERR: Start bit not detected on all data signals in wide bus mode
  *            @arg SDIO_FLAG_DBCKEND:  Data block sent/received (CRC check passed)
  *            @arg SDIO_FLAG_SDIOIT:   SD I/O interrupt received
  *            @arg SDIO_FLAG_CEATAEND: CE-ATA command completion signal received for CMD61
  * @retval None
  */
void SDIO_ClearFlag(uint32_t SDIO_FLAG)
{ 
  /* Check the parameters */
  assert_param(IS_SDIO_CLEAR_FLAG(SDIO_FLAG));
   
  SDIO->ICR = SDIO_FLAG;
}

/**
  * @brief  Checks whether the specified SDIO interrupt has occurred or not.
  * @param  SDIO_IT: specifies the SDIO interrupt source to check. 
  *          This parameter can be one of the following values:
  *            @arg SDIO_IT_CCRCFAIL: Command response received (CRC check failed) interrupt
  *            @arg SDIO_IT_DCRCFAIL: Data block sent/received (CRC check failed) interrupt
  *            @arg SDIO_IT_CTIMEOUT: Command response timeout interrupt
  *            @arg SDIO_IT_DTIMEOUT: Data timeout interrupt
  *            @arg SDIO_IT_TXUNDERR: Transmit FIFO underrun error interrupt
  *            @arg SDIO_IT_RXOVERR:  Received FIFO overrun error interrupt
  *            @arg SDIO_IT_CMDREND:  Command response received (CRC check passed) interrupt
  *            @arg SDIO_IT_CMDSENT:  Command sent (no response required) interrupt
  *            @arg SDIO_IT_DATAEND:  Data end (data counter, SDIDCOUNT, is zero) interrupt
  *            @arg SDIO_IT_STBITERR: Start bit not detected on all data signals in wide 
  *                                   bus mode interrupt
  *            @arg SDIO_IT_DBCKEND:  Data block sent/received (CRC check passed) interrupt
  *            @arg SDIO_IT_CMDACT:   Command transfer in progress interrupt
  *            @arg SDIO_IT_TXACT:    Data transmit in progress interrupt
  *            @arg SDIO_IT_RXACT:    Data receive in progress interrupt
  *            @arg SDIO_IT_TXFIFOHE: Transmit FIFO Half Empty interrupt
  *            @arg SDIO_IT_RXFIFOHF: Receive FIFO Half Full interrupt
  *            @arg SDIO_IT_TXFIFOF:  Transmit FIFO full interrupt
  *            @arg SDIO_IT_RXFIFOF:  Receive FIFO full interrupt
  *            @arg SDIO_IT_TXFIFOE:  Transmit FIFO empty interrupt
  *            @arg SDIO_IT_RXFIFOE:  Receive FIFO empty interrupt
  *            @arg SDIO_IT_TXDAVL:   Data available in transmit FIFO interrupt
  *            @arg SDIO_IT_RXDAVL:   Data available in receive FIFO interrupt
  *            @arg SDIO_IT_SDIOIT:   SD I/O interrupt received interrupt
  *            @arg SDIO_IT_CEATAEND: CE-ATA command completion signal received for CMD61 interrupt
  * @retval The new state of SDIO_IT (SET or RESET).
  */
ITStatus SDIO_GetITStatus(uint32_t SDIO_IT)
{ 
  ITStatus bitstatus = RESET;
  
  /* Check the parameters */
  assert_param(IS_SDIO_GET_IT(SDIO_IT));
  if ((SDIO->STA & SDIO_IT) != (uint32_t)RESET)  
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
  * @brief  Clears the SDIO's interrupt pending bits.
  * @param  SDIO_IT: specifies the interrupt pending bit to clear. 
  *          This parameter can be one or a combination of the following values:
  *            @arg SDIO_IT_CCRCFAIL: Command response received (CRC check failed) interrupt
  *            @arg SDIO_IT_DCRCFAIL: Data block sent/received (CRC check failed) interrupt
  *            @arg SDIO_IT_CTIMEOUT: Command response timeout interrupt
  *            @arg SDIO_IT_DTIMEOUT: Data timeout interrupt
  *            @arg SDIO_IT_TXUNDERR: Transmit FIFO underrun error interrupt
  *            @arg SDIO_IT_RXOVERR:  Received FIFO overrun error interrupt
  *            @arg SDIO_IT_CMDREND:  Command response received (CRC check passed) interrupt
  *            @arg SDIO_IT_CMDSENT:  Command sent (no response required) interrupt
  *            @arg SDIO_IT_DATAEND:  Data end (data counter, SDIO_DCOUNT, is zero) interrupt
  *            @arg SDIO_IT_STBITERR: Start bit not detected on all data signals in wide 
  *                                   bus mode interrupt
  *            @arg SDIO_IT_SDIOIT:   SD I/O interrupt received interrupt
  *            @arg SDIO_IT_CEATAEND: CE-ATA command completion signal received for CMD61
  * @retval None
  */
void SDIO_ClearITPendingBit(uint32_t SDIO_IT)
{ 
  /* Check the parameters */
  assert_param(IS_SDIO_CLEAR_IT(SDIO_IT));
   
  SDIO->ICR = SDIO_IT;
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
